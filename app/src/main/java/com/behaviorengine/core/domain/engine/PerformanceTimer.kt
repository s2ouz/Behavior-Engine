package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Collects timing metrics only — "No optimization required yet" per this phase's spec, so
 * there's deliberately no threshold-checking, no alerting, nothing that reacts to a slow tick.
 * [com.behaviorengine.engine.RuntimeControllerImpl] is the only caller: it wraps
 * [EngineManager.initialize] in [measureStartup] and each tick in [measureTick].
 */
interface PerformanceTimer {

    val snapshot: StateFlow<PerformanceSnapshot>

    /** Runs [block], recording its wall-clock duration as the engine's startup time. */
    fun <T> measureStartup(block: () -> T): T

    /** Runs [block] (one engine tick), recording its duration and folding it into the rolling average. */
    fun <T> measureTick(block: () -> T): T

    /** Records the current total runtime duration, sourced from [EngineClock] rather than re-measured. */
    fun recordRuntimeDuration(durationMillis: Long)

    /** Clears every metric back to defaults; called on [EngineManager.reset]. */
    fun reset()
}
