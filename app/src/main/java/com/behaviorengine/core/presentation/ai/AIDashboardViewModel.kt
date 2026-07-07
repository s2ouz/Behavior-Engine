package com.behaviorengine.core.presentation.ai

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.ai.AIDecisionManager
import com.behaviorengine.core.domain.ai.AIRuntimeStatistics
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.DecisionAction
import com.behaviorengine.core.domain.ai.ExecutionResult
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.ScreenState
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.ai.WorkflowRepository
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTO_RUN_STEP_DELAY_MILLIS = 800L

/** Drives [AIDashboardScreen] — thin delegation to [AIDecisionManager]/[WorkflowRepository]/[TeachingRepository], mirroring [com.behaviorengine.core.presentation.matching.MatchingDebugViewModel]'s role. */
@HiltViewModel
class AIDashboardViewModel @Inject constructor(
    private val aiDecisionManager: AIDecisionManager,
    private val workflowRepository: WorkflowRepository,
    private val teachingRepository: TeachingRepository
) : ViewModel() {

    val isCaptureActive: StateFlow<Boolean> = aiDecisionManager.isCaptureActive
    val isRunning: StateFlow<Boolean> = aiDecisionManager.isRunning
    val currentStepIndex: StateFlow<Int> = aiDecisionManager.currentStepIndex
    val currentScreenState: StateFlow<ScreenState?> = aiDecisionManager.currentScreenState
    val currentPrediction: StateFlow<Prediction?> = aiDecisionManager.currentPrediction
    val currentDecision: StateFlow<Decision?> = aiDecisionManager.currentDecision
    val lastExecutionResult: StateFlow<ExecutionResult?> = aiDecisionManager.lastExecutionResult
    val runtimeStatistics: StateFlow<AIRuntimeStatistics?> = aiDecisionManager.runtimeStatistics

    private val _sessions = MutableStateFlow<List<TeachingSession>>(emptyList())
    val sessions: StateFlow<List<TeachingSession>> = _sessions.asStateFlow()

    private val _workflows = MutableStateFlow<List<Workflow>>(emptyList())
    val workflows: StateFlow<List<Workflow>> = _workflows.asStateFlow()

    private val _selectedWorkflow = MutableStateFlow<Workflow?>(null)
    val selectedWorkflow: StateFlow<Workflow?> = _selectedWorkflow.asStateFlow()

    private val _autoRun = MutableStateFlow(false)
    val autoRun: StateFlow<Boolean> = _autoRun.asStateFlow()

    init {
        refreshSessions()
        refreshWorkflows()
    }

    fun refreshSessions() {
        viewModelScope.launch { _sessions.value = teachingRepository.getSessions() }
    }

    fun refreshWorkflows() {
        viewModelScope.launch { _workflows.value = workflowRepository.listWorkflows() }
    }

    fun onDeriveWorkflowClicked(sessionId: String) {
        viewModelScope.launch {
            workflowRepository.deriveFromSession(sessionId)
            refreshWorkflows()
        }
    }

    fun onSelectWorkflow(workflow: Workflow) {
        _selectedWorkflow.value = workflow
        aiDecisionManager.resetProgress(workflow)
    }

    fun createCaptureIntent(): Intent = aiDecisionManager.createCaptureIntent()

    fun onCaptureGranted(resultCode: Int, data: Intent) = aiDecisionManager.startCapture(resultCode, data)

    fun onStopCaptureClicked() {
        _autoRun.value = false
        aiDecisionManager.stopCapture()
    }

    fun onResetClicked() {
        _autoRun.value = false
        selectedWorkflow.value?.let { aiDecisionManager.resetProgress(it) }
    }

    fun onCancelClicked() {
        _autoRun.value = false
        aiDecisionManager.cancel()
    }

    fun onRunStepClicked() {
        val workflow = selectedWorkflow.value ?: return
        viewModelScope.launch { aiDecisionManager.runStep(workflow) }
    }

    fun onAutoRunToggled(enabled: Boolean) {
        _autoRun.value = enabled
        if (enabled) startAutoRunLoop()
    }

    private fun startAutoRunLoop() {
        val workflow = selectedWorkflow.value ?: return
        viewModelScope.launch {
            while (_autoRun.value) {
                val decision = aiDecisionManager.runStep(workflow)
                if (decision == null || decision.action == DecisionAction.STOP_EXECUTION || decision.action == DecisionAction.ASK_USER_CONFIRMATION) {
                    _autoRun.value = false
                    break
                }
                delay(AUTO_RUN_STEP_DELAY_MILLIS)
            }
        }
    }

    override fun onCleared() {
        aiDecisionManager.cancel()
        super.onCleared()
    }
}
