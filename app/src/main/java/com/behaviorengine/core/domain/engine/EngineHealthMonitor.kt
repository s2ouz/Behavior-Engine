package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Combines [EngineLifecycleManager], [EngineServiceConnection], [EngineObserver] and
 * [ModuleRegistry] into one [EngineHealthSnapshot] so a future diagnostics screen (or crash
 * reporter) has a single thing to subscribe to instead of four.
 */
interface EngineHealthMonitor {
    val snapshot: StateFlow<EngineHealthSnapshot>
}
