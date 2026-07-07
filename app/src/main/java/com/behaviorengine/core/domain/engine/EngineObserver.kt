package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * A standing subscriber to [EventBus] that turns the raw event stream into a running summary
 * (see [EngineObserverSnapshot]) so consumers that just want "is anything wrong?" don't each
 * have to re-implement their own event-counting logic on top of the bus.
 */
interface EngineObserver {
    val snapshot: StateFlow<EngineObserverSnapshot>
}
