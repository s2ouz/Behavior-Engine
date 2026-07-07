package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.PerformanceSnapshot
import com.behaviorengine.core.domain.engine.PerformanceTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [PerformanceTimer]. [tickCount] and [tickDurationSumMillis] are plain
 * `var`s, not atomics — safe only because [measureTick] is exclusively called from
 * [com.behaviorengine.engine.RuntimeControllerImpl]'s single sequential tick loop
 * ([com.behaviorengine.engine.EngineLoopImpl] never runs two ticks concurrently), and
 * [measureStartup] can only run once per session, before RUNNING (and thus before any tick)
 * is reachable. If a future phase ever lets ticks overlap, these need a lock or `AtomicLong`.
 */
@Singleton
class PerformanceTimerImpl @Inject constructor() : PerformanceTimer {

    private val _snapshot = MutableStateFlow(PerformanceSnapshot())
    override val snapshot: StateFlow<PerformanceSnapshot> = _snapshot.asStateFlow()

    private var tickCount: Long = 0L
    private var tickDurationSumMillis: Long = 0L

    override fun <T> measureStartup(block: () -> T): T {
        val startedAtNanos = System.nanoTime()
        val result = block()
        val durationMillis = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLI
        _snapshot.update { it.copy(startupDurationMillis = durationMillis) }
        return result
    }

    override fun <T> measureTick(block: () -> T): T {
        val startedAtNanos = System.nanoTime()
        val result = block()
        val durationMillis = (System.nanoTime() - startedAtNanos) / NANOS_PER_MILLI
        tickCount++
        tickDurationSumMillis += durationMillis
        _snapshot.update {
            it.copy(
                lastTickDurationMillis = durationMillis,
                averageTickDurationMillis = tickDurationSumMillis.toDouble() / tickCount
            )
        }
        return result
    }

    override fun recordRuntimeDuration(durationMillis: Long) {
        _snapshot.update { it.copy(runtimeDurationMillis = durationMillis) }
    }

    override fun reset() {
        tickCount = 0L
        tickDurationSumMillis = 0L
        _snapshot.value = PerformanceSnapshot()
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
