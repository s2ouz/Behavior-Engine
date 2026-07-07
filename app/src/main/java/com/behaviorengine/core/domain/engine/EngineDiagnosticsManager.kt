package com.behaviorengine.core.domain.engine

/**
 * The single entry point for "tell me everything about the engine's current condition" —
 * bundling [EngineMetrics] (numbers), [EngineHealthMonitor] (vitals), [EngineValidator]
 * (invariant checks), and [EngineObserver] (history) into one [DiagnosticsReport] instead of a
 * caller having to know all four exist and combine them itself. [com.behaviorengine.engine.EngineManagerImpl]
 * calls this once after a successful [EngineManager.initialize], logging the result — this phase
 * adds no new UI surface for it, only the automatic self-check on boot.
 */
interface EngineDiagnosticsManager {

    /** Gathers a [DiagnosticsReport] and logs a summary; see [ValidationReport.isValid]. */
    fun runDiagnostics(): DiagnosticsReport
}
