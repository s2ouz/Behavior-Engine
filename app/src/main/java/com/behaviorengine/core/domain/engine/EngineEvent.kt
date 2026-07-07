package com.behaviorengine.core.domain.engine

/**
 * One-shot occurrences published through [EventBus], as opposed to [EngineState] which is a
 * continuous snapshot. [com.behaviorengine.engine.EngineObserverImpl] is the built-in subscriber
 * that aggregates these into a running diagnostic picture; future subsystems (a crash reporter,
 * a debug overlay, automation deciding to back off after repeated errors) can subscribe
 * independently without going through the observer.
 */
sealed class EngineEvent {

    /** Raised on every accepted state change; see [EngineLifecycleManager]. Rejections aren't. */
    data class LifecycleChanged(val from: EngineStatus, val to: EngineStatus) : EngineEvent()

    /** Raised by [ModuleRegistry] whenever a module is registered, removed, enabled or disabled. */
    data class ModuleEvent(val moduleId: String, val type: ModuleEventType) : EngineEvent()

    /** A non-fatal condition worth surfacing, short of a full [EngineError]. */
    data class Warning(val message: String) : EngineEvent()

    /** Raised alongside [EngineLifecycleManager.forceError] whenever the engine hits [EngineError]. */
    data class Error(val error: EngineError) : EngineEvent()

    /** Raised once per tick with the latest reading from [EngineClock]. */
    data class Performance(val tick: Long, val fps: Double) : EngineEvent()
}
