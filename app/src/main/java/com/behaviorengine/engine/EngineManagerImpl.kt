package com.behaviorengine.engine

import com.behaviorengine.core.common.ConfigManager
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.EngineClock
import com.behaviorengine.core.domain.engine.EngineError
import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineLoop
import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.core.domain.engine.EngineModule
import com.behaviorengine.core.domain.engine.EngineState
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.core.domain.engine.EventBus
import com.behaviorengine.core.domain.engine.ModuleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [EngineManager] — the orchestrator that composes every subsystem this
 * phase introduces: [EngineLifecycleManager] for valid state transitions, [EngineClock] for
 * timing, [EngineLoop] for the tick coroutine, and [ModuleRegistry] for fanning lifecycle calls
 * out to registered [EngineModule]s. No subsystem talks to another directly — they only know
 * about [EventBus] — so this class is the one place that knows the *order* things happen in.
 *
 * A module throwing during any lifecycle call is treated as a real engine failure: it's caught,
 * wrapped in [EngineError.ModuleError], published, and forces [EngineStatus.ERROR] via
 * [EngineLifecycleManager.forceError] rather than crashing the app or leaving the engine stuck
 * mid-transition.
 */
@Singleton
class EngineManagerImpl @Inject constructor(
    private val lifecycleManager: EngineLifecycleManager,
    private val clock: EngineClock,
    private val loop: EngineLoop,
    private val moduleRegistry: ModuleRegistry,
    private val eventBus: EventBus,
    private val configManager: ConfigManager,
    private val loggerManager: LoggerManager
) : EngineManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val engineState: StateFlow<EngineState> = combine(
        lifecycleManager.status,
        clock.snapshot
    ) { status, clockSnapshot ->
        EngineState(
            status = status,
            currentTick = clockSnapshot.currentTick,
            currentFps = clockSnapshot.currentFps,
            runningTimeMillis = clockSnapshot.runningTimeMillis,
            uptimeMillis = clockSnapshot.uptimeMillis,
            loadedModules = moduleRegistry.getAllModules().map { it.id }
        )
    }.stateIn(scope, SharingStarted.Eagerly, EngineState())

    override fun initialize() {
        if (!lifecycleManager.transitionTo(EngineStatus.INITIALIZING)) return
        val succeeded = runModules("initialize", moduleRegistry.getAllModules()) { it.initialize() }
        if (succeeded) lifecycleManager.transitionTo(EngineStatus.READY)
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

    override fun reset() {
        val current = lifecycleManager.status.value
        if (current != EngineStatus.STOPPED && current != EngineStatus.ERROR) return
        runModules("release", moduleRegistry.getAllModules()) { it.release() }
        clock.reset()
        lifecycleManager.transitionTo(EngineStatus.OFFLINE)
    }

    private suspend fun onTick() {
        clock.tick()
        val snapshot = clock.snapshot.value
        loggerManager.performance(TAG, "tick=${snapshot.currentTick} fps=${"%.1f".format(snapshot.currentFps)}")
        eventBus.publish(EngineEvent.Performance(tick = snapshot.currentTick, fps = snapshot.currentFps))

        val succeeded = runModules("update", moduleRegistry.getActiveModules()) { it.update() }
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
        const val TAG = "EngineManager"
    }
}
