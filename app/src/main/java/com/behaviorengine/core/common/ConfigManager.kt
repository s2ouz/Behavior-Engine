package com.behaviorengine.core.common

import com.behaviorengine.core.domain.engine.EngineConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime configuration for the engine itself ([EngineConfig]: tick rate, debug/logging flags,
 * future remote config) — as opposed to [com.behaviorengine.settings.AppSettings], which holds
 * *user-facing* preferences edited from the Settings screen. Keeping these separate means a
 * future remote config fetch never has to reason about DataStore, and the Settings screen never
 * has to reason about engine internals.
 *
 * [engineConfig] is in-memory only for now, same as [com.behaviorengine.settings.SettingsManager]
 * — there's no UI yet to edit it, so persistence would have nothing to round-trip against.
 * [com.behaviorengine.engine.EngineManagerImpl] reads it once per [start][EngineManagerImpl.start]
 * to pick the tick rate.
 */
@Singleton
class ConfigManager @Inject constructor() {

    private val _engineConfig = MutableStateFlow(EngineConfig())
    val engineConfig: StateFlow<EngineConfig> = _engineConfig.asStateFlow()

    /** Whether verbose/debug-only behavior should be active. Backed by [AppConstants] for now. */
    fun isDebugMode(): Boolean = AppConstants.DEBUG_MODE
}
