package com.behaviorengine.core.domain.engine

/**
 * Immutable timing readout from [EngineClock]. All four time fields are driven by the same
 * per-tick delta, but reset on different triggers — that's the entire reason they're separate
 * fields rather than one number:
 *
 * @param currentTick Ticks elapsed since the engine was last reset; never decreases except on reset.
 * @param currentFps Instantaneous ticks-per-second, measured from the real gap between the last
 * two ticks (not just an echo of the configured [TickRate]).
 * @param elapsedMillis Wall-clock time since the previous tick — today's raw building block for
 * a future proper deltaTime (seconds, float) API once modules actually consume it.
 * @param runningTimeMillis Time spent RUNNING in the current run; zeroed by [EngineClock.onStopped]
 * when the engine stops.
 * @param uptimeMillis Time spent RUNNING since the engine was last initialized, surviving a
 * stop/restart within the same session; cleared only by [EngineClock.reset].
 */
data class EngineClockSnapshot(
    val currentTick: Long = 0L,
    val currentFps: Double = 0.0,
    val elapsedMillis: Long = 0L,
    val runningTimeMillis: Long = 0L,
    val uptimeMillis: Long = 0L
)
