package com.behaviorengine.core.data.ai

import android.content.Intent
import com.behaviorengine.core.common.AIDecisionLogger
import com.behaviorengine.core.domain.ai.AIDecisionManager
import com.behaviorengine.core.domain.ai.AIRepository
import com.behaviorengine.core.domain.ai.AIRuntimeStatistics
import com.behaviorengine.core.domain.ai.AdaptiveRecoveryEngine
import com.behaviorengine.core.domain.ai.AutomationExecutor
import com.behaviorengine.core.domain.ai.ContextManager
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.DecisionAction
import com.behaviorengine.core.domain.ai.DecisionEngine
import com.behaviorengine.core.domain.ai.ExecutionResult
import com.behaviorengine.core.domain.ai.FailedAttempt
import com.behaviorengine.core.domain.ai.MemoryEngine
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.PredictionEngine
import com.behaviorengine.core.domain.ai.ReasoningEngine
import com.behaviorengine.core.domain.ai.RuntimeContext
import com.behaviorengine.core.domain.ai.ScreenState
import com.behaviorengine.core.domain.ai.StateRecognitionEngine
import com.behaviorengine.core.domain.ai.SuccessfulRoute
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.matching.VisualMatchingManager
import com.behaviorengine.core.domain.teaching.TeachingRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val RECOVERY_CONFIDENCE_THRESHOLD = 50
private val RECOVERY_TRIGGER_ACTIONS = setOf(
    DecisionAction.RETRY_STEP,
    DecisionAction.SEARCH_ALTERNATIVE_OBJECT,
    DecisionAction.GO_BACK
)

/** Real implementation of [AIDecisionManager] — see that interface's KDoc for the pipeline order. */
@Singleton
class AIDecisionManagerImpl @Inject constructor(
    private val visualMatchingManager: VisualMatchingManager,
    private val stateRecognitionEngine: StateRecognitionEngine,
    private val contextManager: ContextManager,
    private val predictionEngine: PredictionEngine,
    private val reasoningEngine: ReasoningEngine,
    private val decisionEngine: DecisionEngine,
    private val adaptiveRecoveryEngine: AdaptiveRecoveryEngine,
    private val automationExecutor: AutomationExecutor,
    private val memoryEngine: MemoryEngine,
    private val aiRepository: AIRepository,
    private val teachingRepository: TeachingRepository,
    private val logger: AIDecisionLogger
) : AIDecisionManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    override val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(0)
    override val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    private val _currentScreenState = MutableStateFlow<ScreenState?>(null)
    override val currentScreenState: StateFlow<ScreenState?> = _currentScreenState.asStateFlow()

    private val _currentContext = MutableStateFlow<RuntimeContext?>(null)
    override val currentContext: StateFlow<RuntimeContext?> = _currentContext.asStateFlow()

    private val _currentPrediction = MutableStateFlow<Prediction?>(null)
    override val currentPrediction: StateFlow<Prediction?> = _currentPrediction.asStateFlow()

    private val _currentDecision = MutableStateFlow<Decision?>(null)
    override val currentDecision: StateFlow<Decision?> = _currentDecision.asStateFlow()

    private val _lastExecutionResult = MutableStateFlow<ExecutionResult?>(null)
    override val lastExecutionResult: StateFlow<ExecutionResult?> = _lastExecutionResult.asStateFlow()

    private val _runtimeStatistics = MutableStateFlow<AIRuntimeStatistics?>(null)
    override val runtimeStatistics: StateFlow<AIRuntimeStatistics?> = _runtimeStatistics.asStateFlow()

    override val isCaptureActive: StateFlow<Boolean> = visualMatchingManager.isCaptureActive

    private var stepsCompleted = 0
    private var stepsFailed = 0
    private var recoveriesUsed = 0
    private val decisionLatenciesMillis = mutableListOf<Long>()

    override fun createCaptureIntent(): Intent = visualMatchingManager.createCaptureIntent()

    override fun startCapture(resultCode: Int, data: Intent) = visualMatchingManager.startCapture(resultCode, data)

    override fun stopCapture() = visualMatchingManager.stopCapture()

    override fun resetProgress(workflow: Workflow) {
        _currentStepIndex.value = 0
        _currentScreenState.value = null
        _currentContext.value = null
        _currentPrediction.value = null
        _currentDecision.value = null
        _lastExecutionResult.value = null
        _runtimeStatistics.value = null
        stepsCompleted = 0
        stepsFailed = 0
        recoveriesUsed = 0
        decisionLatenciesMillis.clear()
    }

    override suspend fun analyzeScreen(workflow: Workflow): ScreenState? {
        val screen = visualMatchingManager.captureLiveScreen() ?: return null
        val screenState = stateRecognitionEngine.recognize(screen, workflow)
        logger.stateRecognized(screenState.screenType.name, screenState.confidence)
        _currentScreenState.value = screenState

        val activePackage = runCatching { teachingRepository.loadSession(workflow.sourceSessionId)?.packageName }.getOrNull().orEmpty()
        val context = contextManager.buildContext(workflow, _currentStepIndex.value, screenState, activePackage)
        _currentContext.value = context
        return screenState
    }

    override suspend fun predictNextStep(workflow: Workflow): Prediction? {
        val context = _currentContext.value ?: return null
        val prediction = predictionEngine.predict(context, workflow)
        logger.prediction(prediction.expectedScreen?.name, prediction.probability)
        aiRepository.savePrediction(prediction)
        _currentPrediction.value = prediction
        return prediction
    }

    override suspend fun generatePlan(workflow: Workflow): Decision? {
        val context = _currentContext.value ?: return null
        val prediction = _currentPrediction.value ?: return null
        val startTime = System.currentTimeMillis()

        val reasoning = reasoningEngine.reason(context, workflow, prediction)
        var decision = decisionEngine.decide(context, workflow, prediction, reasoning)

        if (decision.confidence < RECOVERY_CONFIDENCE_THRESHOLD || decision.action in RECOVERY_TRIGGER_ACTIONS) {
            decision = adaptiveRecoveryEngine.recover(context, workflow, decision)
            recoveriesUsed++
        }

        decisionLatenciesMillis.add(System.currentTimeMillis() - startTime)
        logger.decisionGenerated(decision.action.name, decision.confidence)
        aiRepository.saveDecision(decision)
        _currentDecision.value = decision
        return decision
    }

    override suspend fun executeDecision(workflow: Workflow): ExecutionResult? {
        val decision = _currentDecision.value ?: return null
        val context = _currentContext.value ?: return null
        val result = automationExecutor.execute(decision, context)
        _lastExecutionResult.value = result
        return result
    }

    override suspend fun evaluateResult(workflow: Workflow) {
        val decision = _currentDecision.value ?: return
        val context = _currentContext.value ?: return

        when (decision.action) {
            DecisionAction.CONTINUE_WORKFLOW -> {
                stepsCompleted++
                memoryEngine.recordSuccess(
                    SuccessfulRoute(
                        id = UUID.randomUUID().toString(),
                        workflowId = workflow.id,
                        stepIndex = context.currentStep,
                        templateId = workflow.steps.getOrNull(context.currentStep)?.templateId.orEmpty(),
                        confidence = decision.confidence,
                        recordedAtMillis = System.currentTimeMillis()
                    )
                )
                logger.memoryUpdated("successful route")
                _currentStepIndex.value = context.currentStep + 1
            }
            DecisionAction.RESTART_WORKFLOW -> _currentStepIndex.value = 0
            DecisionAction.STOP_EXECUTION -> saveRuntimeStatistics(workflow)
            else -> {
                stepsFailed++
                memoryEngine.recordFailure(
                    FailedAttempt(
                        id = UUID.randomUUID().toString(),
                        workflowId = workflow.id,
                        stepIndex = context.currentStep,
                        reason = decision.reason,
                        recordedAtMillis = System.currentTimeMillis()
                    )
                )
                logger.memoryUpdated("failed attempt")
            }
        }

        if (context.currentStep + 1 >= workflow.steps.size && decision.action == DecisionAction.CONTINUE_WORKFLOW) {
            saveRuntimeStatistics(workflow)
        }
    }

    override suspend fun runStep(workflow: Workflow): Decision? {
        val deferred = CompletableDeferred<Decision?>()
        job = scope.launch {
            _isRunning.value = true
            try {
                if (_isPaused.value) {
                    deferred.complete(null)
                    return@launch
                }
                analyzeScreen(workflow)
                predictNextStep(workflow)
                val decision = generatePlan(workflow)
                executeDecision(workflow)
                evaluateResult(workflow)
                deferred.complete(decision)
            } catch (c: kotlinx.coroutines.CancellationException) {
                deferred.complete(null)
                throw c
            } catch (t: Throwable) {
                logger.error("runStep failed", t)
                deferred.completeExceptionally(t)
            } finally {
                _isRunning.value = false
            }
        }
        return deferred.await()
    }

    override fun cancel() {
        job?.cancel()
    }

    override fun pause() {
        _isPaused.value = true
    }

    override fun resume() {
        _isPaused.value = false
    }

    private suspend fun saveRuntimeStatistics(workflow: Workflow) {
        val stats = AIRuntimeStatistics(
            id = UUID.randomUUID().toString(),
            workflowId = workflow.id,
            stepsCompleted = stepsCompleted,
            stepsFailed = stepsFailed,
            recoveriesUsed = recoveriesUsed,
            averageDecisionLatencyMillis = if (decisionLatenciesMillis.isEmpty()) 0L else decisionLatenciesMillis.average().toLong(),
            recordedAtMillis = System.currentTimeMillis()
        )
        aiRepository.saveRuntime(stats)
        _runtimeStatistics.value = stats
    }
}
