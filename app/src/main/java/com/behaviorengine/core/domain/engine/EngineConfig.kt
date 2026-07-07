package com.behaviorengine.core.domain.engine

import com.behaviorengine.core.common.AppConstants

/**
 * Engine-internal runtime configuration, held by [com.behaviorengine.core.common.ConfigManager].
 * Only [targetTickRate] actually drives behavior this phase (read by
 * [com.behaviorengine.engine.EngineManagerImpl.start] to size the tick interval); the rest are
 * declared now so later phases don't have to reshape this data class, matching how
 * [com.behaviorengine.settings.AppSettings] reserved fields for AI/accessibility ahead of time.
 */
data class EngineConfig(
    val targetTickRate: TickRate = TickRate.FPS_10,
    val debugEnabled: Boolean = AppConstants.DEBUG_MODE,
    val loggingEnabled: Boolean = true,
    val performanceMonitorEnabled: Boolean = true,
    /** Reserved: whether the engine should call initialize()+start() automatically on app launch. */
    val autoStart: Boolean = false,
    val aiEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val visionEnabled: Boolean = false
)
