package com.behaviorengine.core.domain.engine

/**
 * An on-demand metrics port, deliberately pull-based rather than a `StateFlow` like
 * [EngineStateStore]. [EngineStateStore] exists so Compose can *observe* state continuously;
 * this exists for the opposite shape of consumer — a one-off diagnostics dump
 * ([EngineDiagnosticsManager]), a log line on error, or a future test assertion — that just
 * wants "the numbers, right now" without subscribing to anything or leaking a collector.
 */
interface EngineMetrics {

    /** Computes a fresh [EngineMetricsSnapshot] from current values; never cached. */
    fun snapshot(): EngineMetricsSnapshot
}
