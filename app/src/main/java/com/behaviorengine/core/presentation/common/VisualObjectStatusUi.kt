package com.behaviorengine.core.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.behaviorengine.R
import com.behaviorengine.core.domain.objects.VisualObjectStatus
import com.behaviorengine.ui.theme.StatusError
import com.behaviorengine.ui.theme.StatusIdle
import com.behaviorengine.ui.theme.StatusRunning
import com.behaviorengine.ui.theme.StatusTraining

/**
 * The one place [VisualObjectStatus] maps to its label and color — shared by `ObjectCard` and
 * `ObjectDetailsScreen` so the two can never disagree about what a status looks like.
 */
@Composable
fun VisualObjectStatus.displayLabel(): String = when (this) {
    VisualObjectStatus.READY -> stringResource(R.string.status_ready)
    VisualObjectStatus.DISABLED -> stringResource(R.string.status_disabled)
    VisualObjectStatus.TRAINING -> stringResource(R.string.status_training)
    VisualObjectStatus.ARCHIVED -> stringResource(R.string.status_archived)
}

/** Green/Yellow/Gray/Red — the only four colors this phase's spec allows for status. */
fun VisualObjectStatus.displayColor(): Color = when (this) {
    VisualObjectStatus.READY -> StatusRunning
    VisualObjectStatus.TRAINING -> StatusTraining
    VisualObjectStatus.DISABLED -> StatusIdle
    VisualObjectStatus.ARCHIVED -> StatusError
}
