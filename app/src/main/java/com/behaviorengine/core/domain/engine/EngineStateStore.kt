package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth every UI component observes. Before this phase, the engine's own
 * ViewModel read `EngineManager.engineState` directly; now `EngineManager` only exposes actions
 * ([EngineManager.initialize], [EngineManager.start], ...) and every piece of observable state
 * — not just [EngineState] but [EngineSession], [EngineHealthSnapshot], and
 * [PerformanceSnapshot] too — comes from here instead. No UI class should assemble or own any
 * of this state itself; it should only ever forward what this store already computed.
 */
interface EngineStateStore {

    /** What the engine is doing right now: status, tick, fps, running/uptime, loaded modules. */
    val engineState: StateFlow<EngineState>

    /** Identity and vitals of the current run; see [EngineSession]. */
    val session: StateFlow<EngineSession>

    /** Aggregated health vitals; see [EngineHealthMonitor]. */
    val health: StateFlow<EngineHealthSnapshot>

    /** Timing metrics; see [PerformanceTimer]. */
    val performance: StateFlow<PerformanceSnapshot>
}
