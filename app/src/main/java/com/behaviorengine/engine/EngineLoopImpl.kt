package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineLoop
import com.behaviorengine.core.domain.engine.TickRate
import com.behaviorengine.di.EngineCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [EngineLoop]; see that interface for the `while (isActive)` rationale. */
@Singleton
class EngineLoopImpl @Inject constructor(
    @EngineCoroutineScope private val scope: CoroutineScope
) : EngineLoop {

    private var job: Job? = null

    override val isRunning: Boolean
        get() = job?.isActive == true

    override fun start(tickRate: TickRate, onTick: suspend () -> Unit) {
        if (isRunning) return
        job = scope.launch {
            while (isActive) {
                delay(tickRate.intervalMillis)
                onTick()
            }
        }
    }

    override fun stop() {
        job?.cancel()
        job = null
    }
}
