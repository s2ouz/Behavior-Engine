package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [EventBus]; see that interface for why there's no listener API. */
@Singleton
class EventBusImpl @Inject constructor() : EventBus {

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = EVENT_BUFFER_CAPACITY)
    override val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    override fun publish(event: EngineEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val EVENT_BUFFER_CAPACITY = 64
    }
}
