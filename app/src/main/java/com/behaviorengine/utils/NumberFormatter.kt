package com.behaviorengine.utils

import java.util.Locale

/**
 * Decimal formatting for the small set of numeric readouts the engine surfaces (fps, timing in
 * milliseconds) — kept separate from [TimeFormatter], which formats *durations* as `HH:mm:ss`
 * rather than a plain decimal. Always pins [Locale.US] so a device with a different default
 * locale (e.g. one using `,` as a decimal separator) can't turn a logged fps value into
 * something a log parser or screenshot comparison doesn't expect.
 */
object NumberFormatter {

    /** Formats a fps reading to one decimal place, e.g. `9.8`. */
    fun formatFps(fps: Double): String = String.format(Locale.US, "%.1f", fps)

    /** Formats a millisecond duration to two decimal places with a unit suffix, e.g. `1.23 ms`. */
    fun formatMillis(millis: Double): String = String.format(Locale.US, "%.2f ms", millis)
}
