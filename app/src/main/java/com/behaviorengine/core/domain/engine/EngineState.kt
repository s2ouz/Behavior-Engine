package com.behaviorengine.core.domain.engine

import com.behaviorengine.core.common.AppConstants

/**
 * Immutable snapshot of the engine at a single point in time.
 *
 * This is the one object the presentation layer observes; every future subsystem that wants
 * to surface something to the UI (a vision frame rate, a learning confidence score, etc.)
 * should extend this snapshot rather than exposing a second, competing source of truth.
 *
 * @param status Current lifecycle state, see [EngineStatus].
 * @param currentPhase Human-readable development phase (e.g. "Foundation"), shown in the UI.
 * @param runningTimeMillis Milliseconds the engine has been continuously RUNNING since the
 * last start(). Reset on stop().
 * @param version Engine core version, independent of the app's own version.
 * @param reserved Free-form slot for fields future phases need without breaking this data
 * class's shape (avoids a churn of consumer-breaking changes while the engine is young).
 */
data class EngineState(
    val status: EngineStatus = EngineStatus.OFFLINE,
    val currentPhase: String = AppConstants.CURRENT_PHASE,
    val runningTimeMillis: Long = 0L,
    val version: String = AppConstants.ENGINE_VERSION,
    val reserved: Map<String, String> = emptyMap()
)
