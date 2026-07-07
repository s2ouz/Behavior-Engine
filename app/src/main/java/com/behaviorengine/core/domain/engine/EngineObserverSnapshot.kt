package com.behaviorengine.core.domain.engine

/**
 * Aggregated diagnostic picture maintained by [EngineObserver] from the raw [EngineEvent] stream.
 * Distinct from [EngineState]: [EngineState] is "what the engine is doing right now" (what the
 * Home screen binds to), this is "what has happened so far" (for a future diagnostics screen,
 * crash reporter, or automation that wants to back off after repeated errors).
 */
data class EngineObserverSnapshot(
    val lastLifecycleChange: EngineEvent.LifecycleChanged? = null,
    val moduleEventCount: Int = 0,
    val warningCount: Int = 0,
    val lastWarning: String? = null,
    val errorCount: Int = 0,
    val lastError: EngineError? = null,
    val lastPerformance: EngineEvent.Performance? = null
)
