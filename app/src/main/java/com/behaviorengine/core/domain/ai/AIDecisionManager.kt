package com.behaviorengine.core.domain.ai

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Top-level orchestrator — "analyze current state, determine next action, detect abnormal
 * situations, coordinate AI components," per spec. Runs the pipeline in
 * [com.behaviorengine.core.data.ai.AIDecisionManagerImpl]'s actual order: capture screen →
 * [StateRecognitionEngine] → [ContextManager] → [PredictionEngine] → [ReasoningEngine] →
 * [DecisionEngine] → (low confidence? → [AdaptiveRecoveryEngine]) → [AutomationExecutor] →
 * result evaluation → [MemoryEngine] update.
 *
 * Each spec-named function below does exactly the one pipeline stage it's named for and is real,
 * independently callable — [com.behaviorengine.core.presentation.ai.AIDashboardScreen] calls them
 * individually so a developer can inspect every intermediate result, not just the final action.
 */
interface AIDecisionManager {

    val isRunning: StateFlow<Boolean>
    val isPaused: StateFlow<Boolean>
    val currentStepIndex: StateFlow<Int>
    val currentScreenState: StateFlow<ScreenState?>
    val currentContext: StateFlow<RuntimeContext?>
    val currentPrediction: StateFlow<Prediction?>
    val currentDecision: StateFlow<Decision?>
    val lastExecutionResult: StateFlow<ExecutionResult?>
    val runtimeStatistics: StateFlow<AIRuntimeStatistics?>

    /** True while a `MediaProjection` capture session is active — shared with [com.behaviorengine.core.domain.matching.VisualMatchingManager], same underlying singleton. */
    val isCaptureActive: StateFlow<Boolean>

    fun createCaptureIntent(): Intent

    fun startCapture(resultCode: Int, data: Intent)

    fun stopCapture()

    /** Resets step progress and per-run counters for a fresh attempt at [workflow]. */
    fun resetProgress(workflow: Workflow)

    suspend fun analyzeScreen(workflow: Workflow): ScreenState?

    suspend fun predictNextStep(workflow: Workflow): Prediction?

    suspend fun generatePlan(workflow: Workflow): Decision?

    suspend fun executeDecision(workflow: Workflow): ExecutionResult?

    suspend fun evaluateResult(workflow: Workflow)

    /** Runs one full pipeline pass — analyze → predict → plan → execute → evaluate — and returns the [Decision] made. */
    suspend fun runStep(workflow: Workflow): Decision?

    fun cancel()

    fun pause()

    fun resume()
}
