package com.behaviorengine.core.domain.engine

/**
 * Timing readout from [PerformanceTimer]. Deliberately separate from [EngineClockSnapshot]:
 * the clock answers "how much tick time has the simulation experienced" (driven by the
 * configured [TickRate] interval), this answers "how fast is our own code actually running" —
 * a genuine profiling concern that only gets more important once real modules do real work
 * inside [EngineModule.update].
 *
 * @param startupDurationMillis How long the last [EngineManager.initialize] call took; null
 * until the engine has been initialized at least once.
 * @param lastTickDurationMillis Wall-clock time the most recent tick's own work took to execute
 * (clock bookkeeping + module updates) — should stay well under [TickRate.intervalMillis].
 * @param averageTickDurationMillis Rolling average of every tick duration since the last
 * [PerformanceTimer.reset].
 * @param runtimeDurationMillis Mirrors [EngineClockSnapshot.uptimeMillis]; recorded here rather
 * than independently measured so the two numbers can never disagree.
 */
data class PerformanceSnapshot(
    val startupDurationMillis: Long? = null,
    val lastTickDurationMillis: Long = 0L,
    val averageTickDurationMillis: Double = 0.0,
    val runtimeDurationMillis: Long = 0L
)
