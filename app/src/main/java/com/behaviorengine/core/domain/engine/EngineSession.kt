package com.behaviorengine.core.domain.engine

/**
 * Identity and vitals of the current engine run, from [EngineManager.initialize] through
 * [EngineManager.reset]. Distinct from [EngineState]: that answers "what is the engine doing,"
 * this answers "which run is this, and how long has it been going" — useful the moment a future
 * phase wants to correlate logs, metrics, or persisted history back to a specific session
 * rather than just "the engine" as an undifferentiated whole.
 *
 * [com.behaviorengine.engine.EngineStateStoreImpl] mints a new [sessionId] the moment the
 * engine leaves OFFLINE for INITIALIZING, and clears this back to defaults the moment it
 * returns to OFFLINE via [EngineManager.reset] — it never resets on a mere [EngineManager.stop].
 *
 * @param sessionId Random UUID identifying this run; empty when no session is active.
 * @param startTimeMillis Wall-clock time ([System.currentTimeMillis]) the session began.
 * @param elapsedTimeMillis Wall-clock time since [startTimeMillis]; 0 when no session is active.
 * @param status Mirrors [EngineState.status] for convenience so a session view doesn't need a
 * second subscription just to know what state its own session is in.
 * @param currentTick Mirrors [EngineState.currentTick].
 * @param currentFps Mirrors [EngineState.currentFps].
 * @param reserved Free-form slot for future per-session statistics (e.g. total actions taken).
 */
data class EngineSession(
    val sessionId: String = "",
    val startTimeMillis: Long = 0L,
    val elapsedTimeMillis: Long = 0L,
    val status: EngineStatus = EngineStatus.OFFLINE,
    val currentTick: Long = 0L,
    val currentFps: Double = 0.0,
    val reserved: Map<String, String> = emptyMap()
)
