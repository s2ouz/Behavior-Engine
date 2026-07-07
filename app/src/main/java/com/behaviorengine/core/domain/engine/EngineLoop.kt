package com.behaviorengine.core.domain.engine

/**
 * A restartable, cancellable coroutine ticker — the "permanent engine loop" this phase asks for,
 * without ever writing a literal `while (true)`: the implementation loops on `while (isActive)`
 * so cancelling the underlying coroutine is enough to stop it, no extra flag plumbing needed.
 * Deliberately generic (it doesn't know about [EngineClock] or [ModuleRegistry]) so
 * [com.behaviorengine.engine.EngineManagerImpl] can supply whatever tick behavior the engine
 * currently needs, and so other future subsystems needing their own independent tick rate
 * (e.g. a vision capture loop) can reuse it.
 */
interface EngineLoop {

    /** Whether a tick coroutine is currently active. */
    val isRunning: Boolean

    /** Starts ticking at [tickRate], invoking [onTick] on each interval. No-ops if already running. */
    fun start(tickRate: TickRate, onTick: suspend () -> Unit)

    /** Cancels the current tick coroutine, if any. Safe to call when not running. */
    fun stop()
}
