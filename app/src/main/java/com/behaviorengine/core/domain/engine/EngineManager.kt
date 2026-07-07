package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Domain-layer contract for controlling the engine's lifecycle.
 *
 * Deliberately Android-framework-free (no Context, no Application dependency) so it can be
 * unit tested and, longer term, so the domain layer never depends on how the engine is actually
 * implemented. [com.behaviorengine.engine.EngineManagerImpl] is where real behavior lives; this
 * interface is what the rest of the app (ViewModels, future automation code) is allowed to see.
 */
interface EngineManager {

    /** Continuously observable snapshot of the engine; see [EngineState]. */
    val engineState: StateFlow<EngineState>

    /** Transitions the engine from OFFLINE towards RUNNING. */
    fun start()

    /** Transitions the engine from RUNNING/PAUSED towards OFFLINE. */
    fun stop()

    /** Suspends engine activity without a full stop; only valid while RUNNING. */
    fun pause()

    /** Resumes engine activity after a [pause]; only valid while PAUSED. */
    fun resume()
}
