package com.behaviorengine.core.presentation.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.behaviorengine.R
import com.behaviorengine.core.presentation.common.PlaceholderScreen

/**
 * Placeholder per this phase's spec — [com.behaviorengine.settings.SettingsManager] and
 * [com.behaviorengine.settings.AppSettings] are prepared, but no editable preference UI exists
 * yet. The one working link ("Engine Diagnostics") is navigation, not a preference — the same
 * exception Home's own cards get — kept here so the fully-tested engine control screen from
 * v0.1.0–v0.5.0 stays reachable instead of becoming unreferenced dead code.
 */
@Composable
fun SettingsScreen(onEngineDiagnosticsClick: () -> Unit, onMatchingDebugClick: () -> Unit, onAIDashboardClick: () -> Unit) {
    PlaceholderScreen(
        title = stringResource(R.string.settings_title),
        description = stringResource(R.string.settings_placeholder)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onEngineDiagnosticsClick) {
            Text(stringResource(R.string.settings_engine_diagnostics_button))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onMatchingDebugClick) {
            Text(stringResource(R.string.settings_matching_debug_button))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onAIDashboardClick) {
            Text(stringResource(R.string.settings_ai_dashboard_button))
        }
    }
}
