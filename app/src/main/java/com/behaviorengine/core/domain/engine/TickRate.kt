package com.behaviorengine.core.domain.engine

import com.behaviorengine.core.common.AppConstants

/**
 * Supported engine tick rates. [FPS_10] is the default per this phase's spec — low enough that
 * logcat stays readable while there's nothing but log lines happening each tick. Higher rates
 * are prepared for later phases where module work per tick actually costs something.
 */
enum class TickRate(val fps: Int) {
    FPS_10(10),
    FPS_30(30),
    FPS_60(60),
    FPS_120(120);

    /** Delay between ticks in milliseconds, derived from [fps] so the two can never disagree. */
    val intervalMillis: Long get() = AppConstants.MILLIS_PER_SECOND / fps
}
