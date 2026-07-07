package com.behaviorengine.core.common

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime configuration for the engine itself (feature flags, tuning values, future remote
 * config) — as opposed to [com.behaviorengine.settings.AppSettings], which holds *user-facing*
 * preferences edited from the Settings screen. Keeping these separate means a future remote
 * config fetch never has to reason about DataStore, and the Settings screen never has to reason
 * about engine internals.
 *
 * No-op today: there is no engine behavior yet to configure. Exists so [EngineManagerImpl]
 * and future subsystems have a single injectable place to ask "is X enabled?" instead of each
 * inventing its own flag source.
 */
@Singleton
class ConfigManager @Inject constructor() {

    /** Whether verbose/debug-only behavior should be active. Backed by [AppConstants] for now. */
    fun isDebugMode(): Boolean = AppConstants.DEBUG_MODE
}
