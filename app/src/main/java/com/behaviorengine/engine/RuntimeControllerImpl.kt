package com.behaviorengine.engine

import com.behaviorengine.core.common.ConfigManager
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.EngineClock
import com.behaviorengine.core.domain.engine.EngineError
import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineLoop
import com.behaviorengine.core.domain.engine.EngineModule
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.core.domain.engine.EventBus
import com.behaviorengine.core.domain.engine.ModuleRegistry
import com.behaviorengine.core.domain.engine.PerformanceTimer
import com.behaviorengine.core.domain.engine.RuntimeController
import com.behaviorengine.core.domain.engine.canReset
import com.behaviorengine.utils.NumberFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [RuntimeController] — the direct successor to v0.2.0's
 * `EngineManagerImpl` body, moved here unchanged in behavior so that phase's guarantees
 * (illegal transitions rejected, a broken module forces ERROR instead of crashing) carry
 * forward untouched. The only additions are [PerformanceTimer] hooks around startup and each
 * tick, since nothing here previously measured its own execution time.
 */
@Singleton
class RuntimeControllerImpl @Inject constructor(
    private val lifecycleManager: EngineLifecycleManager,
    private val clock: EngineClock,
    private val loop: EngineLoop,
    private val moduleRegistry: ModuleRegistry,
    private val eventBus: EventBus,
    private val configManager: ConfigManager,
    private val performanceTimer: PerformanceTimer,
    private val loggerManager: LoggerManager
) : RuntimeController {

    override fun initialize(): Boolean {
        if (!lifecycleManager.transitionTo(EngineStatus.INITIALIZING)) return false
        val succeeded = performanceTimer.measureStartup {
            runModules("initialize", moduleRegistry.getAllModules()) { it.initialize() }
        }
        if (succeeded) lifecycleManager.transitionTo(EngineStatus.READY)
        return succeeded
    }

    override fun start() {
        if (!lifecycleManager.transitionTo(EngineStatus.STARTING)) return
        val succeeded = runModules("start", moduleRegistry.getActiveModules()) { it.start() }
        if (!succeeded) return
        clock.onRunningStateChanged(isRunning = true)
        loop.start(configManager.engineConfig.value.targetTickRate) { onTick() }
        lifecycleManager.transitionTo(EngineStatus.RUNNING)
    }

    override fun pause() {
        if (!lifecycleManager.transitionTo(EngineStatus.PAUSING)) return
        loop.stop()
        clock.onRunningStateChanged(isRunning = false)
        val succeeded = runModules("pause", moduleRegistry.getActiveModules()) { it.pause() }
        if (succeeded) lifecycleManager.transitionTo(EngineStatus.PAUSED)
    }

    override fun resume() {
        if (!lifecycleManager.transitionTo(EngineStatus.RESUMING)) return
        val succeeded = runModules("resume", moduleRegistry.getActiveModules()) { it.resume() }
        if (!succeeded) return
        clock.onRunningStateChanged(isRunning = true)
        loop.start(configManager.engineConfig.value.targetTickRate) { onTick() }
        lifecycleManager.transitionTo(EngineStatus.RUNNING)
    }

    override fun stop() {
        if (!lifecycleManager.transitionTo(EngineStatus.STOPPING)) return
        loop.stop()
        clock.onRunningStateChanged(isRunning = false)
        val succeeded = runModules("stop", moduleRegistry.getActiveModules()) { it.stop() }
        clock.onStopped()
        if (succeeded) lifecycleManager.transitionTo(EngineStatus.STOPPED)
    }

    override fun reset(): Boolean {
        if (!lifecycleManager.status.value.canReset()) return false
        runModules("release", moduleRegistry.getAllModules()) { it.release() }
        clock.reset()
        performanceTimer.reset()
        return lifecycleManager.transitionTo(EngineStatus.OFFLINE)
    }

    private suspend fun onTick() {
        val succeeded = performanceTimer.measureTick {
            clock.tick()
            runModules("update", moduleRegistry.getActiveModules()) { it.update() }
        }

        val snapshot = clock.snapshot.value
        performanceTimer.recordRuntimeDuration(snapshot.uptimeMillis)
        eventBus.publish(EngineEvent.Performance(tick = snapshot.currentTick, fps = snapshot.currentFps))

        // Logged at most once per second regardless of tick rate — at FPS_120 a per-tick log
        // line would mean 120 logcat writes/sec for no extra signal; the EventBus publish above
        // already carries every tick's reading for anything that needs the full-resolution data.
        val ticksPerSecond = configManager.engineConfig.value.targetTickRate.fps.toLong()
        if (snapshot.currentTick % ticksPerSecond == 0L) {
            loggerManager.performance(TAG, "tick=${snapshot.currentTick} fps=${NumberFormatter.formatFps(snapshot.currentFps)}")
        }

        if (!succeeded) loop.stop()
    }

    /**
     * Runs [action] against each module in priority order, isolating failures per module so one
     * broken module produces a precise [EngineError.ModuleError] instead of an opaque crash.
     * Returns false (and forces ERROR) on the first failure, skipping the remaining modules.
     */
    private inline fun runModules(
        stage: String,
        modules: List<EngineModule>,
        action: (EngineModule) -> Unit
    ): Boolean {
        for (module in modules) {
            try {
                action(module)
            } catch (t: Throwable) {
                val error = EngineError.ModuleError(
                    moduleId = module.id,
                    message = "Module '${module.id}' failed during $stage: ${t.message}"
                )
                loggerManager.e(TAG, error.message, t)
                lifecycleManager.forceError(error)
                return false
            }
        }
        return true
    }

    private companion object {
        const val TAG = "RuntimeController"
    }
}
