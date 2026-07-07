package com.behaviorengine.core.presentation.teaching

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.behaviorengine.R
import com.behaviorengine.core.presentation.common.PlaceholderScreen

/**
 * Will start capturing and naming new visual objects once [com.behaviorengine.vision] (screen
 * capture) and [com.behaviorengine.recognition] exist. Placeholder per this phase's spec.
 */
@Composable
fun TeachingScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.teaching_title),
        description = stringResource(R.string.teaching_placeholder)
    )
}
