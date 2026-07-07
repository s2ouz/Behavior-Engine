package com.behaviorengine.core.presentation.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.common.AppConstants
import com.behaviorengine.core.domain.engine.EngineHealthSnapshot
import com.behaviorengine.core.domain.engine.EngineSession
import com.behaviorengine.core.domain.engine.EngineState
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.core.domain.engine.PerformanceSnapshot
import com.behaviorengine.core.domain.engine.canInitialize
import com.behaviorengine.core.domain.engine.canPause
import com.behaviorengine.core.domain.engine.canReset
import com.behaviorengine.core.domain.engine.canResume
import com.behaviorengine.core.domain.engine.canStart
import com.behaviorengine.core.domain.engine.canStop
import com.behaviorengine.ui.theme.StatusError
import com.behaviorengine.ui.theme.StatusIdle
import com.behaviorengine.ui.theme.StatusRunning
import com.behaviorengine.utils.NumberFormatter
import com.behaviorengine.utils.TimeFormatter

/**
 * The engine's own status/control screen — displays and drives
 * [com.behaviorengine.core.domain.engine.EngineManager] directly. Not part of the product's main
 * flow (Home/Objects/Teaching/Automation); reachable only from Settings as a diagnostics tool,
 * since end users of a visual automation product shouldn't need to see raw tick counts or FSM
 * states to use the app.
 */
@Composable
fun EngineScreen(viewModel: EngineViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val engineState by viewModel.engineState.collectAsState()
    val session by viewModel.session.collectAsState()
    val health by viewModel.health.collectAsState()
    val performance by viewModel.performance.collectAsState()

    EngineContent(
        engineState = engineState,
        session = session,
        health = health,
        performance = performance,
        onBackClick = onBackClick,
        onInitializeClicked = viewModel::onInitializeClicked,
        onStartClicked = viewModel::onStartClicked,
        onPauseClicked = viewModel::onPauseClicked,
        onResumeClicked = viewModel::onResumeClicked,
        onStopClicked = viewModel::onStopClicked,
        onResetClicked = viewModel::onResetClicked
    )
}

@Composable
private fun EngineContent(
    engineState: EngineState,
    session: EngineSession,
    health: EngineHealthSnapshot,
    performance: PerformanceSnapshot,
    onBackClick: () -> Unit,
    onInitializeClicked: () -> Unit,
    onStartClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onResumeClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = onBackClick) {
                Text(stringResource(R.string.engine_back_button))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(horizontal = 32.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = AppConstants.PROJECT_NAME,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.home_version_label, engineState.version),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            InfoRow(
                label = stringResource(R.string.home_status_label),
                value = engineState.status.name,
                valueColor = engineState.status.toDisplayColor()
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_runtime_status_label),
                value = runtimeStatusLabel(engineState.status)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_service_status_label),
                value = serviceStatusLabel(health.serviceConnected)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_session_id_label),
                value = sessionIdLabel(session.sessionId)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_running_time_label),
                value = TimeFormatter.formatElapsed(engineState.runningTimeMillis)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_tick_count_label),
                value = engineState.currentTick.toString()
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.home_avg_tick_time_label),
                value = NumberFormatter.formatMillis(performance.averageTickDurationMillis)
            )

            Spacer(modifier = Modifier.height(40.dp))

            EngineControls(
                status = engineState.status,
                onInitializeClicked = onInitializeClicked,
                onStartClicked = onStartClicked,
                onPauseClicked = onPauseClicked,
                onResumeClicked = onResumeClicked,
                onStopClicked = onStopClicked,
                onResetClicked = onResetClicked
            )
        }
    }
}

@Composable
private fun EngineControls(
    status: EngineStatus,
    onInitializeClicked: () -> Unit,
    onStartClicked: () -> Unit,
    onPauseClicked: () -> Unit,
    onResumeClicked: () -> Unit,
    onStopClicked: () -> Unit,
    onResetClicked: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onInitializeClicked,
                enabled = status.canInitialize()
            ) {
                Text(stringResource(R.string.home_initialize_button))
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = onStartClicked,
                enabled = status.canStart()
            ) {
                Text(stringResource(R.string.home_start_button))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onPauseClicked,
                enabled = status.canPause()
            ) {
                Text(stringResource(R.string.home_pause_button))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onResumeClicked,
                enabled = status.canResume()
            ) {
                Text(stringResource(R.string.home_resume_button))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onStopClicked,
                enabled = status.canStop()
            ) {
                Text(stringResource(R.string.home_stop_button))
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onResetClicked,
                enabled = status.canReset()
            ) {
                Text(stringResource(R.string.home_reset_button))
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onBackground
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun runtimeStatusLabel(status: EngineStatus): String = when (status) {
    EngineStatus.RUNNING -> stringResource(R.string.home_runtime_active)
    EngineStatus.PAUSED -> stringResource(R.string.home_runtime_paused)
    else -> stringResource(R.string.home_runtime_idle)
}

@Composable
private fun serviceStatusLabel(connected: Boolean): String =
    stringResource(if (connected) R.string.home_service_connected else R.string.home_service_disconnected)

@Composable
private fun sessionIdLabel(sessionId: String): String =
    sessionId.takeIf { it.isNotEmpty() }?.take(SESSION_ID_DISPLAY_LENGTH) ?: stringResource(R.string.home_session_id_none)

private const val SESSION_ID_DISPLAY_LENGTH = 8

private fun EngineStatus.toDisplayColor(): Color = when (this) {
    EngineStatus.RUNNING -> StatusRunning
    EngineStatus.ERROR -> StatusError
    else -> StatusIdle
}
