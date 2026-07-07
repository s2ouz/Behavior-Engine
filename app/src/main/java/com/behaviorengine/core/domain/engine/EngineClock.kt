package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * The engine's sense of time: tick count, measured FPS, and the three duration fields on
 * [EngineClockSnapshot]. Kept separate from [EngineLoop] on purpose — the loop is just "a thing
 * that fires periodically and can be stopped/restarted," while the clock is the stateful record
 * of what happened across however many start/pause/resume cycles occur before the next reset.
 */
interface EngineClock {

    val snapshot: StateFlow<EngineClockSnapshot>

    /** Advances the clock by one tick; called from [EngineLoop]'s tick callback, never directly by UI. */
    fun tick()

    /**
     * Tells the clock whether ticks currently count as "running" time, and resynchronizes its
     * internal timestamp so a long pause isn't misread as one enormous tick delta on resume.
     */
    fun onRunningStateChanged(isRunning: Boolean)

    /** Zeroes [EngineClockSnapshot.runningTimeMillis] only; tick count, fps and uptime are untouched. */
    fun onStopped()

    /** Clears every field back to defaults; called when the engine returns to OFFLINE. */
    fun reset()
}
