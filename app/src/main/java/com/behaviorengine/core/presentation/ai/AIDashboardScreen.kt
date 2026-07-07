package com.behaviorengine.core.presentation.ai

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.domain.ai.AIRuntimeStatistics
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.ScreenState
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.presentation.common.InfoRow

private const val CARD_CORNER_RADIUS_DP = 16

/**
 * Development/debugging dashboard for SPEC-13's Adaptive AI Decision Engine — reachable only from
 * Settings, same reasoning as `MatchingDebugScreen`/`EngineScreen`. Since there's no
 * workflow-authoring UI, this screen also drives [com.behaviorengine.core.domain.ai.WorkflowRepository.deriveFromSession] —
 * pick a finished teaching session, derive a workflow from it, then step (or auto-step) the AI
 * through it, inspecting every intermediate result: recognized screen, decision + confidence +
 * alternatives, prediction, and running statistics.
 */
@Composable
fun AIDashboardScreen(viewModel: AIDashboardViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val context = LocalContext.current
    val isCaptureActive by viewModel.isCaptureActive.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val workflows by viewModel.workflows.collectAsState()
    val selectedWorkflow by viewModel.selectedWorkflow.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val screenState by viewModel.currentScreenState.collectAsState()
    val prediction by viewModel.currentPrediction.collectAsState()
    val decision by viewModel.currentDecision.collectAsState()
    val runtimeStatistics by viewModel.runtimeStatistics.collectAsState()
    val autoRun by viewModel.autoRun.collectAsState()

    var hasNotificationPermission by remember { mutableStateOf(hasNotificationPermission(context)) }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) viewModel.onCaptureGranted(result.resultCode, data)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    fun onStartCaptureClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            mediaProjectionLauncher.launch(viewModel.createCaptureIntent())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onBackClick) { Text(stringResource(R.string.ai_dashboard_back_button)) }
            }
            Text(
                text = stringResource(R.string.ai_dashboard_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ai_dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!isCaptureActive) {
            item {
                InfoCard(stringResource(R.string.ai_dashboard_capture_inactive))
                Button(onClick = ::onStartCaptureClick, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.matching_debug_start_capture_button))
                }
            }
        } else {
            item {
                OutlinedButton(onClick = viewModel::onStopCaptureClicked, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.matching_debug_stop_capture_button))
                }
            }

            item { SectionTitle(stringResource(R.string.ai_dashboard_sessions_title)) }
            if (sessions.isEmpty()) {
                item { Text(stringResource(R.string.ai_dashboard_no_sessions), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(session = session, onDeriveClick = { viewModel.onDeriveWorkflowClicked(session.id) })
                }
            }

            item { SectionTitle(stringResource(R.string.ai_dashboard_workflows_title)) }
            if (workflows.isEmpty()) {
                item { Text(stringResource(R.string.ai_dashboard_no_workflows), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(workflows, key = { it.id }) { workflow ->
                    WorkflowRow(
                        workflow = workflow,
                        isSelected = workflow.id == selectedWorkflow?.id,
                        onSelectClick = { viewModel.onSelectWorkflow(workflow) }
                    )
                }
            }

            selectedWorkflow?.let { workflow ->
                item {
                    RunControls(
                        stepIndex = currentStepIndex,
                        totalSteps = workflow.steps.size,
                        isRunning = isRunning,
                        autoRun = autoRun,
                        onRunStepClick = viewModel::onRunStepClicked,
                        onAutoRunToggled = viewModel::onAutoRunToggled,
                        onResetClick = viewModel::onResetClicked,
                        onCancelClick = viewModel::onCancelClicked
                    )
                }
                item { ScreenStateCard(screenState) }
                item { DecisionCard(decision) }
                item { PredictionCard(prediction) }
                item { RuntimeCard(runtimeStatistics) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun InfoCard(text: String) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(text = text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SessionRow(session: TeachingSession, onDeriveClick: () -> Unit) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = session.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${session.touchCount} touches", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onDeriveClick) { Text(stringResource(R.string.ai_dashboard_derive_button)) }
        }
    }
}

@Composable
private fun WorkflowRow(workflow: Workflow, isSelected: Boolean, onSelectClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = workflow.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "${workflow.steps.size} steps", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onSelectClick) { Text(stringResource(R.string.ai_dashboard_select_button)) }
        }
    }
}

@Composable
private fun RunControls(
    stepIndex: Int,
    totalSteps: Int,
    isRunning: Boolean,
    autoRun: Boolean,
    onRunStepClick: () -> Unit,
    onAutoRunToggled: (Boolean) -> Unit,
    onResetClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow(stringResource(R.string.ai_dashboard_step_label), "$stepIndex / $totalSteps")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.ai_dashboard_auto_run_label))
                Switch(checked = autoRun, onCheckedChange = onAutoRunToggled)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRunStepClick, enabled = !isRunning) { Text(stringResource(R.string.ai_dashboard_run_step_button)) }
                OutlinedButton(onClick = onResetClick) { Text(stringResource(R.string.ai_dashboard_reset_button)) }
                OutlinedButton(onClick = onCancelClick) { Text(stringResource(R.string.ai_dashboard_cancel_button)) }
            }
        }
    }
}

@Composable
private fun ScreenStateCard(screenState: ScreenState?) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(stringResource(R.string.ai_dashboard_screen_state_title))
            if (screenState == null) {
                Text(stringResource(R.string.ai_dashboard_not_yet_analyzed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                InfoRow(stringResource(R.string.ai_dashboard_screen_type_label), screenState.screenType.name)
                InfoRow(stringResource(R.string.ai_dashboard_confidence_label), "${screenState.confidence}%")
                InfoRow(stringResource(R.string.ai_dashboard_detected_objects_label), screenState.detectedObjects.size.toString())
            }
        }
    }
}

@Composable
private fun DecisionCard(decision: Decision?) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(stringResource(R.string.ai_dashboard_decision_title))
            if (decision == null) {
                Text(stringResource(R.string.ai_dashboard_not_yet_analyzed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                InfoRow(stringResource(R.string.ai_dashboard_action_label), decision.action.name)
                InfoRow(stringResource(R.string.ai_dashboard_confidence_label), "${decision.confidence}%")
                Text(text = decision.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = decision.expectedOutcome, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (decision.alternatives.isNotEmpty()) {
                    Text(stringResource(R.string.ai_dashboard_alternatives_label), style = MaterialTheme.typography.labelMedium)
                    decision.alternatives.forEach { alt ->
                        Text(text = "${alt.action.name}: ${alt.confidence}% — ${alt.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PredictionCard(prediction: Prediction?) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(stringResource(R.string.ai_dashboard_prediction_title))
            if (prediction == null) {
                Text(stringResource(R.string.ai_dashboard_not_yet_analyzed), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                InfoRow(stringResource(R.string.ai_dashboard_expected_object_label), prediction.expectedObjectTemplateId?.take(8) ?: "—")
                InfoRow(stringResource(R.string.ai_dashboard_probability_label), "${(prediction.probability * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun RuntimeCard(statistics: AIRuntimeStatistics?) {
    Card(shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionTitle(stringResource(R.string.ai_dashboard_runtime_title))
            if (statistics == null) {
                Text(stringResource(R.string.ai_dashboard_no_runtime_yet), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                InfoRow(stringResource(R.string.ai_dashboard_steps_completed_label), statistics.stepsCompleted.toString())
                InfoRow(stringResource(R.string.ai_dashboard_steps_failed_label), statistics.stepsFailed.toString())
                InfoRow(stringResource(R.string.ai_dashboard_recoveries_label), statistics.recoveriesUsed.toString())
                InfoRow(stringResource(R.string.ai_dashboard_avg_latency_label), "${statistics.averageDecisionLatencyMillis}ms")
            }
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
