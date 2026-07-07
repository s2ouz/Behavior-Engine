package com.behaviorengine.core.presentation.objects

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.behaviorengine.R
import com.behaviorengine.core.presentation.common.PlaceholderScreen

/**
 * Will list and manage taught visual objects once [com.behaviorengine.recognition] and
 * [com.behaviorengine.memory] exist to supply and store them. Placeholder per this phase's spec.
 */
@Composable
fun ObjectsScreen() {
    PlaceholderScreen(
        title = stringResource(R.string.objects_title),
        description = stringResource(R.string.objects_placeholder)
    )
}
