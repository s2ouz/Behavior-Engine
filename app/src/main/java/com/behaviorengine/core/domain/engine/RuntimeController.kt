package com.behaviorengine.core.domain.engine

/**
 * Owns the actual tick-loop mechanics: fanning [EngineModule] lifecycle calls out via
 * [ModuleRegistry], driving [EngineLoop] and [EngineClock] in lockstep, and validating every
 * transition through [EngineLifecycleManager]. This is exactly what
 * [com.behaviorengine.engine.EngineManagerImpl] did on its own in v0.2.0; it's extracted here
 * so [EngineManager] can stay a thin facade as it grows a second, orthogonal responsibility
 * this phase (owning [EngineServiceConnection]) without either responsibility bloating a
 * single class.
 *
 * "Keep runtime state synchronized" (this phase's spec) means: [EngineClock] is told about
 * running/stopped transitions at exactly the same moments [EngineLifecycleManager] commits
 * them, so nothing downstream can ever observe one having moved without the other.
 */
interface RuntimeController {

    /** Fans out module initialize() calls (OFFLINE → INITIALIZING → READY). Returns success. */
    fun initialize(): Boolean

    /** Starts the tick loop (READY → STARTING → RUNNING). */
    fun start()

    /** Stops the tick loop without a full stop (RUNNING → PAUSING → PAUSED). */
    fun pause()

    /** Restarts the tick loop after [pause] (PAUSED → RESUMING → RUNNING). */
    fun resume()

    /** Halts the tick loop (RUNNING/PAUSED → STOPPING → STOPPED). */
    fun stop()

    /** Releases modules and clears the clock (STOPPED/ERROR → OFFLINE). Returns success. */
    fun reset(): Boolean
}
