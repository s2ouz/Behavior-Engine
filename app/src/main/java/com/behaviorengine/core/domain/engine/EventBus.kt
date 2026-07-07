package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.SharedFlow

/**
 * The engine's pub/sub channel for [EngineEvent]s. Deliberately Flow-based rather than a
 * hand-rolled listener list: "subscribe" is collecting [events] inside a coroutine scope, and
 * "unsubscribe" is cancelling that scope/job — Kotlin's structured concurrency already gives us
 * both for free, so a parallel add/removeListener API would just be a second, redundant way to
 * do the same thing. [com.behaviorengine.engine.EngineObserverImpl] is the reference consumer.
 */
interface EventBus {

    /** Hot stream of every event the engine has published since this app process started. */
    val events: SharedFlow<EngineEvent>

    /** Publishes an event to all current subscribers; silently dropped if there are none. */
    fun publish(event: EngineEvent)
}
