package com.behaviorengine.core.domain.engine

/**
 * A single, point-in-time readout combining every numeric signal the engine exposes —
 * [EngineClock] timing, [PerformanceTimer] profiling, and [EngineHealthMonitor] counts — in one
 * object. See [EngineMetrics] for why this exists alongside [EngineStateStore] rather than
 * replacing it.
 */
data class EngineMetricsSnapshot(
    val status: EngineStatus,
    val currentTick: Long,
    val currentFps: Double,
    val runningTimeMillis: Long,
    val uptimeMillis: Long,
    val startupDurationMillis: Long?,
    val lastTickDurationMillis: Long,
    val averageTickDurationMillis: Double,
    val moduleCount: Int,
    val errorCount: Int,
    val warningCount: Int
)
