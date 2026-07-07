package com.behaviorengine.core.domain.engine

/** Everything [EngineDiagnosticsManager.runDiagnostics] gathers in one call, bundled for logging or export. */
data class DiagnosticsReport(
    val metrics: EngineMetricsSnapshot,
    val health: EngineHealthSnapshot,
    val validation: ValidationReport,
    val observed: EngineObserverSnapshot
)
