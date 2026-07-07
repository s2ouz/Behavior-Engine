package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineClock
import com.behaviorengine.core.domain.engine.EngineClockSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineClockImpl @Inject constructor() : EngineClock {

    private val _snapshot = MutableStateFlow(EngineClockSnapshot())
    override val snapshot: StateFlow<EngineClockSnapshot> = _snapshot.asStateFlow()

    private var lastTickAtMillis: Long = 0L
    private var isRunning: Boolean = false

    override fun tick() {
        val now = System.currentTimeMillis()
        val delta = if (lastTickAtMillis == 0L) 0L else now - lastTickAtMillis
        lastTickAtMillis = now
        val fps = if (delta > 0) MILLIS_PER_SECOND / delta.toDouble() else 0.0

        _snapshot.update { current ->
            current.copy(
                currentTick = current.currentTick + 1,
                currentFps = fps,
                elapsedMillis = delta,
                runningTimeMillis = current.runningTimeMillis + if (isRunning) delta else 0L,
                uptimeMillis = current.uptimeMillis + if (isRunning) delta else 0L
            )
        }
    }

    override fun onRunningStateChanged(isRunning: Boolean) {
        this.isRunning = isRunning
        // Discards the stale timestamp so the next tick's delta is just the tick interval,
        // not the entire pause/idle gap misread as one giant tick.
        if (isRunning) lastTickAtMillis = System.currentTimeMillis()
    }

    override fun onStopped() {
        _snapshot.update { it.copy(runningTimeMillis = 0L) }
    }

    override fun reset() {
        lastTickAtMillis = 0L
        isRunning = false
        _snapshot.value = EngineClockSnapshot()
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
