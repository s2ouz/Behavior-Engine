package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineLoop
import com.behaviorengine.core.domain.engine.TickRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineLoopImpl @Inject constructor() : EngineLoop {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
