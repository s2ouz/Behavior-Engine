package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the engine's state machine: the single place that decides whether a transition between
 * [EngineStatus] values is legal, so [com.behaviorengine.engine.EngineManagerImpl] never has to
 * re-derive the rules itself. For example RUNNING can move to PAUSING or STOPPING, but never
 * directly back to INITIALIZING — attempting that must be rejected, not silently coerced.
 */
interface EngineLifecycleManager {

    /** The current, always-valid state; every accepted transition publishes [EngineEvent.LifecycleChanged]. */
    val status: StateFlow<EngineStatus>

    /** True if moving from the current status to [target] is a legal transition right now. */
    fun canTransitionTo(target: EngineStatus): Boolean

    /**
     * Attempts to move to [target]. Returns true and updates [status] if legal; returns false
     * and leaves [status] untouched otherwise — illegal transitions are rejected, not corrected.
     */
    fun transitionTo(target: EngineStatus): Boolean

    /**
     * Unconditionally forces [EngineStatus.ERROR], bypassing the normal transition table. Used
     * only when a module or the engine itself has already failed (see [EngineError]) — at that
     * point there's nothing left to validate, the engine simply needs to land somewhere safe.
     */
    fun forceError(error: EngineError)
}
