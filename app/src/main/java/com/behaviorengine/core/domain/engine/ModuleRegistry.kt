package com.behaviorengine.core.domain.engine

/**
 * Owns every registered [EngineModule] and the order the engine deals with them in. Registering
 * a module doesn't start it — that only happens when [EngineLifecycleManager] moves through
 * INITIALIZING/STARTING and [RuntimeController] fans out to [getAllModules] / [getActiveModules]
 * — so a module can be registered ahead of time and later enabled/disabled without
 * re-registering it.
 */
interface ModuleRegistry {

    /** Adds or replaces the module under its own [EngineModule.id]. */
    fun register(module: EngineModule)

    /** Removes a module entirely; a no-op if [moduleId] isn't registered. */
    fun remove(moduleId: String)

    /** Marks a registered module as active without removing it; a no-op if not registered. */
    fun enable(moduleId: String)

    /** Marks a registered module as inactive without removing it; a no-op if not registered. */
    fun disable(moduleId: String)

    /** Looks up a module by id regardless of its enabled state, or null if not registered. */
    fun find(moduleId: String): EngineModule?

    /** Enabled modules only, ordered by [ModulePriority] — what the engine actually drives each tick. */
    fun getActiveModules(): List<EngineModule>

    /** Every registered module regardless of enabled state, ordered by [ModulePriority]. */
    fun getAllModules(): List<EngineModule>
}
