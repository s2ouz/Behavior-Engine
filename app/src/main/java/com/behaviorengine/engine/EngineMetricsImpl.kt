package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineClock
import com.behaviorengine.core.domain.engine.EngineHealthMonitor
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineMetrics
import com.behaviorengine.core.domain.engine.EngineMetricsSnapshot
import com.behaviorengine.core.domain.engine.ModuleRegistry
import com.behaviorengine.core.domain.engine.PerformanceTimer
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [EngineMetrics]; see that interface for why it's pull- not push-based. */
@Singleton
class EngineMetricsImpl @Inject constructor(
    private val lifecycleManager: EngineLifecycleManager,
    private val clock: EngineClock,
    private val performanceTimer: PerformanceTimer,
    private val moduleRegistry: ModuleRegistry,
    private val engineHealthMonitor: EngineHealthMonitor
) : EngineMetrics {

    override fun snapshot(): EngineMetricsSnapshot {
        val clockSnapshot = clock.snapshot.value
        val performanceSnapshot = performanceTimer.snapshot.value
        val health = engineHealthMonitor.snapshot.value

        return EngineMetricsSnapshot(
            status = lifecycleManager.status.value,
            currentTick = clockSnapshot.currentTick,
            currentFps = clockSnapshot.currentFps,
            runningTimeMillis = clockSnapshot.runningTimeMillis,
            uptimeMillis = clockSnapshot.uptimeMillis,
            startupDurationMillis = performanceSnapshot.startupDurationMillis,
            lastTickDurationMillis = performanceSnapshot.lastTickDurationMillis,
            averageTickDurationMillis = performanceSnapshot.averageTickDurationMillis,
            moduleCount = moduleRegistry.getAllModules().size,
            errorCount = health.errorCount,
            warningCount = health.warningCount
        )
    }
}
