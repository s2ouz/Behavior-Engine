package com.behaviorengine.utils

import com.behaviorengine.core.common.AppConstants
import java.util.Locale

/**
 * Small formatting helpers with no domain knowledge of their own — the kind of thing that would
 * otherwise get copy-pasted into whichever screen needs it first. Distinct from
 * [com.behaviorengine.core.common], which holds app-wide managers rather than pure functions.
 */
object TimeFormatter {

    /** Formats milliseconds as `HH:mm:ss`, e.g. the engine's running time on the Home screen. */
    fun formatElapsed(millis: Long): String {
        val totalSeconds = millis / AppConstants.MILLIS_PER_SECOND
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
