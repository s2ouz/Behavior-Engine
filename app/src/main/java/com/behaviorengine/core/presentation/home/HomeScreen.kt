package com.behaviorengine.core.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.common.AppConstants
import com.behaviorengine.core.domain.engine.EngineState
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.ui.theme.StatusError
import com.behaviorengine.ui.theme.StatusIdle
import com.behaviorengine.ui.theme.StatusRunning
import com.behaviorengine.utils.TimeFormatter

/** The only functional screen in the Foundation phase: displays and drives [EngineManager][com.behaviorengine.core.domain.engine.EngineManager]. */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val engineState by viewModel.engineState.collectAsState()

    HomeContent(
        engineState = engineState,
        onStartClicked = viewModel::onStartClicked,
        onStopClicked = viewModel::onStopClicked
    )
}

@Composable
private fun HomeContent(
    engineState: EngineState,
    onStartClicked: () -> Unit,
    onStopClicked: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
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

            Spacer(modifier = Modifier.height(40.dp))

            InfoRow(
                label = stringResource(R.string.home_status_label),
                value = engineState.status.name,
                valueColor = engineState.status.toDisplayColor()
            )
            Spacer(modifier = Modifier.height(16.dp))
            InfoRow(
                label = stringResource(R.string.home_phase_label),
                value = engineState.currentPhase
            )

            if (engineState.status == EngineStatus.RUNNING) {
                Spacer(modifier = Modifier.height(16.dp))
                InfoRow(
                    label = stringResource(R.string.home_running_time_label),
                    value = TimeFormatter.formatElapsed(engineState.runningTimeMillis)
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onStartClicked,
                    enabled = engineState.status == EngineStatus.OFFLINE || engineState.status == EngineStatus.ERROR
                ) {
                    Text(stringResource(R.string.home_start_button))
                }

                OutlinedButton(
                    onClick = onStopClicked,
                    enabled = engineState.status == EngineStatus.RUNNING
                ) {
                    Text(stringResource(R.string.home_stop_button))
                }
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun EngineStatus.toDisplayColor(): Color = when (this) {
    EngineStatus.RUNNING -> StatusRunning
    EngineStatus.ERROR -> StatusError
    else -> StatusIdle
}
