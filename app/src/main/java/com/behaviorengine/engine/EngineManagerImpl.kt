package com.behaviorengine.engine

import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.core.domain.engine.EngineState
import com.behaviorengine.core.domain.engine.EngineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [EngineManager].
 *
 * This is the orchestrator future phases will wire vision/recognition/world/behavior modules
 * into (start() will initialize them, stop() will tear them down). For this foundation phase
 * there is nothing to orchestrate yet, so lifecycle methods only manage [EngineStatus] and a
 * running-time clock — enough to prove the StateFlow -> ViewModel -> Compose UI pipeline works
 * end to end without hardcoding any UI-side assumptions about the engine's internals.
 */
@Singleton
class EngineManagerImpl @Inject constructor(
    private val loggerManager: LoggerManager
) : EngineManager {

    private val _engineState = MutableStateFlow(EngineState())
    override val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var clockJob: Job? = null

    override fun start() {
        if (_engineState.value.status !in STARTABLE_STATUSES) return
        loggerManager.i(TAG, "Engine starting")
        _engineState.update { it.copy(status = EngineStatus.STARTING, runningTimeMillis = 0L) }
        _engineState.update { it.copy(status = EngineStatus.RUNNING) }
        startClock()
    }

    override fun stop() {
        if (_engineState.value.status == EngineStatus.OFFLINE) return
        loggerManager.i(TAG, "Engine stopping")
        _engineState.update { it.copy(status = EngineStatus.STOPPING) }
        stopClock()
        _engineState.update { it.copy(status = EngineStatus.OFFLINE, runningTimeMillis = 0L) }
    }

    override fun pause() {
        if (_engineState.value.status != EngineStatus.RUNNING) return
        loggerManager.i(TAG, "Engine paused")
        stopClock()
        _engineState.update { it.copy(status = EngineStatus.PAUSED) }
    }

    override fun resume() {
        if (_engineState.value.status != EngineStatus.PAUSED) return
        loggerManager.i(TAG, "Engine resumed")
        _engineState.update { it.copy(status = EngineStatus.RUNNING) }
        startClock()
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = scope.launch {
            while (true) {
                delay(CLOCK_TICK_MILLIS)
                _engineState.update { it.copy(runningTimeMillis = it.runningTimeMillis + CLOCK_TICK_MILLIS) }
            }
        }
    }

    private fun stopClock() {
        clockJob?.cancel()
        clockJob = null
    }

    private companion object {
        const val TAG = "EngineManager"
        const val CLOCK_TICK_MILLIS = 1000L
        val STARTABLE_STATUSES = setOf(EngineStatus.OFFLINE, EngineStatus.ERROR)
    }
}
