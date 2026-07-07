package com.behaviorengine.core.domain.engine

/**
 * One-shot occurrences the engine can raise, as opposed to [EngineState] which is a continuous
 * snapshot. Not yet emitted anywhere — no subsystem exists to raise anything interesting yet —
 * but declared now so later phases (vision frame captured, rule matched, automation failed...)
 * plug into an already-agreed event shape instead of inventing a new channel each time.
 */
sealed class EngineEvent {
    data object Started : EngineEvent()
    data object Stopped : EngineEvent()
    data object Paused : EngineEvent()
    data object Resumed : EngineEvent()
    data class Error(val message: String) : EngineEvent()
}
