package com.behaviorengine.core.domain.engine

import com.behaviorengine.core.common.AppConstants

/**
 * Immutable snapshot of the engine at a single point in time — the one object the presentation
 * layer observes. Combines [EngineLifecycleManager.status] with [EngineClockSnapshot] and the
 * currently registered module ids so the Home screen has everything it needs from a single
 * [EngineManager.engineState] subscription instead of juggling several sources.
 *
 * @param status Current lifecycle state, see [EngineStatus].
 * @param currentPhase Human-readable development phase (e.g. "Core Infrastructure"), shown in the UI.
 * @param version Engine core version, independent of the app's own version.
 * @param currentTick See [EngineClockSnapshot.currentTick].
 * @param currentFps See [EngineClockSnapshot.currentFps].
 * @param runningTimeMillis See [EngineClockSnapshot.runningTimeMillis].
 * @param uptimeMillis See [EngineClockSnapshot.uptimeMillis].
 * @param loadedModules Ids of every module currently known to [ModuleRegistry], active or not.
 * @param reserved Free-form slot for fields future phases need without breaking this data
 * class's shape (avoids a churn of consumer-breaking changes while the engine is young).
 */
data class EngineState(
    val status: EngineStatus = EngineStatus.OFFLINE,
    val currentPhase: String = AppConstants.CURRENT_PHASE,
    val version: String = AppConstants.ENGINE_VERSION,
    val currentTick: Long = 0L,
    val currentFps: Double = 0.0,
    val runningTimeMillis: Long = 0L,
    val uptimeMillis: Long = 0L,
    val loadedModules: List<String> = emptyList(),
    val reserved: Map<String, String> = emptyMap()
)
