package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-layer facade for controlling the engine. This is the *only* engine type the
 * presentation layer (ViewModels) is allowed to depend on — everything this phase adds
 * ([EngineLifecycleManager], [EngineClock], [EngineLoop], [ModuleRegistry], [EventBus],
 * [EngineObserver]) is internal machinery [com.behaviorengine.engine.EngineManagerImpl]
 * composes behind this one contract, so a UI change never needs to know that composition grew.
 *
 * Deliberately Android-framework-free (no Context, no Application dependency) so it can be
 * unit tested and so the domain layer never depends on how the engine is actually implemented.
 *
 * Method names mirror [EngineStatus] one-for-one: [initialize] drives OFFLINE→READY,
 * [start] drives READY→RUNNING, [pause]/[resume] toggle RUNNING↔PAUSED, [stop] drives
 * RUNNING/PAUSED→STOPPED, and [reset] drives STOPPED/ERROR back to OFFLINE.
 */
interface EngineManager {

    /** Continuously observable snapshot of the engine; see [EngineState]. */
    val engineState: StateFlow<EngineState>

    /** Prepares registered modules (OFFLINE → INITIALIZING → READY). */
    fun initialize()

    /** Begins ticking (READY → STARTING → RUNNING). */
    fun start()

    /** Suspends ticking without a full stop (RUNNING → PAUSING → PAUSED). */
    fun pause()

    /** Resumes ticking after a [pause] (PAUSED → RESUMING → RUNNING). */
    fun resume()

    /** Halts ticking (RUNNING/PAUSED → STOPPING → STOPPED). */
    fun stop()

    /** Releases modules and clears the clock, returning to OFFLINE (STOPPED/ERROR → OFFLINE). */
    fun reset()
}
