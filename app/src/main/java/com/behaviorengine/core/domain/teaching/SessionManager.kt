package com.behaviorengine.core.domain.teaching

/**
 * Session lifecycle + metadata, wrapping [TeachingRepository] — "Managers coordinate everything;
 * Repositories only read/write data." [TeachingModeManager] is the only intended caller.
 */
interface SessionManager {

    /** Builds a new [TeachingSession] stamped with current device/screen metadata, in [TeachingState.PREPARING], and saves it. */
    suspend fun createSession(): TeachingSession

    /** Moves [sessionId] to [TeachingState.RECORDING], stamping [TeachingSession.startedAtMillis] on the first call. */
    suspend fun startSession(sessionId: String): TeachingSession

    /** Moves [sessionId] to [TeachingState.PAUSED]. */
    suspend fun pauseSession(sessionId: String): TeachingSession

    /** Moves [sessionId] back to [TeachingState.RECORDING]. */
    suspend fun resumeSession(sessionId: String): TeachingSession

    /** Moves [sessionId] to [TeachingState.COMPLETED], stamping [TeachingSession.finishedAtMillis]/[TeachingSession.durationMillis]. */
    suspend fun finishSession(sessionId: String): TeachingSession

    /** Moves [sessionId] to [TeachingState.CANCELLED], stamping [TeachingSession.finishedAtMillis]/[TeachingSession.durationMillis]. */
    suspend fun cancelSession(sessionId: String): TeachingSession

    suspend fun loadSession(sessionId: String): TeachingSession?

    suspend fun deleteSession(sessionId: String)

    /** Bumps [TeachingSession.frameCount] by one and persists the change. */
    suspend fun incrementFrameCount(sessionId: String): TeachingSession

    /** Bumps [TeachingSession.touchCount] by one and persists the change. */
    suspend fun incrementTouchCount(sessionId: String): TeachingSession
}
