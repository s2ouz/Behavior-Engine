package com.behaviorengine.core.domain.engine

/**
 * The contract every engine subsystem will implement: a future Vision module, Rule module,
 * Learning module, Memory module, Action module, and Feedback module all plug into
 * [com.behaviorengine.engine.EngineManagerImpl] through this single shape, registered with a
 * [ModuleRegistry]. None of those modules exist yet — this phase only establishes the interface
 * they'll implement and the registry that will hold them.
 *
 * Lifecycle calls mirror [EngineLifecycleManager]'s states one-for-one: [initialize] runs during
 * ENGINE INITIALIZING, [start]/[stop] during STARTING/STOPPING, [update] once per engine tick
 * while RUNNING, [pause]/[resume] during PAUSING/RESUMING, and [release] when the engine resets
 * back to OFFLINE.
 */
interface EngineModule {

    /** Stable identifier used by [ModuleRegistry] to find, enable, disable, or remove this module. */
    val id: String

    /** Determines this module's position in [ModuleRegistry.getAllModules] init/start order. */
    val priority: ModulePriority

    fun initialize()
    fun start()
    fun update()
    fun pause()
    fun resume()
    fun stop()
    fun release()
}
