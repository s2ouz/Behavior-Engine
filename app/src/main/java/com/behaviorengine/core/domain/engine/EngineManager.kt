package com.behaviorengine.core.domain.engine

/**
 * Domain-layer facade for controlling the engine. This is the *only* engine type the
 * presentation layer's actions are allowed to depend on: [com.behaviorengine.engine.EngineManagerImpl]
 * composes [RuntimeController] (the tick loop) and [EngineServiceConnection] (the durable
 * background host) behind this one contract. Observable state is a separate concern — see
 * [EngineStateStore] — so a UI class talks to this interface to *act* and to
 * [EngineStateStore] to *observe*, never assembling state of its own from either.
 *
 * [EngineManager] is also the *only* class allowed to call [EngineServiceConnection.connect] /
 * [EngineServiceConnection.disconnect]: nothing else, not a ViewModel and not
 * [com.behaviorengine.services.EngineService] itself, should decide when the background host
 * starts or stops. That decision is tied to the session boundary, which only [initialize] and
 * [reset] know about.
 *
 * Method names mirror [EngineStatus] one-for-one: [initialize] drives OFFLINE→READY (and, on
 * success, connects the background host), [start] drives READY→RUNNING, [pause]/[resume] toggle
 * RUNNING↔PAUSED, [stop] drives RUNNING/PAUSED→STOPPED, and [reset] drives STOPPED/ERROR back to
 * OFFLINE (and, on success, disconnects the background host — this is this phase's "destroy
 * runtime").
 */
interface EngineManager {

    /** Prepares registered modules and connects the background host (OFFLINE → READY). */
    fun initialize()

    /** Begins ticking (READY → RUNNING). */
    fun start()

    /** Suspends ticking without a full stop (RUNNING → PAUSED). */
    fun pause()

    /** Resumes ticking after a [pause] (PAUSED → RUNNING). */
    fun resume()

    /** Halts ticking (RUNNING/PAUSED → STOPPED). */
    fun stop()

    /** Releases modules, clears the clock, and disconnects the background host (→ OFFLINE). */
    fun reset()
}
