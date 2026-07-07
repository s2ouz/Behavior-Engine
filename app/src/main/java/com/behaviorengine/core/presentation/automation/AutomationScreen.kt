package com.behaviorengine.core.presentation.automation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.behaviorengine.R
import com.behaviorengine.core.presentation.common.PlaceholderScreen

/**
 * Will start/stop object automation once [com.behaviorengine.behavior] (rules/actions) and
 * [com.behaviorengine.automation] (execution) exist. Placeholder per this phase's spec — this is
 * a different concern from [com.behaviorengine.core.presentation.engine.EngineScreen], which
 * controls the engine's own runtime, not automation *of taught objects*.
 */
@Composable
fun AutomationScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.automation_title),
        description = stringResource(R.string.automation_placeholder)
    )
}
