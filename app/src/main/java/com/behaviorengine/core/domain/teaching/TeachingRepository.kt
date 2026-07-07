package com.behaviorengine.core.domain.teaching

import kotlinx.coroutines.flow.StateFlow

/**
 * Business-facing read/write seam over [TeachingStorage] — "Repositories only read/write data,"
 * per this phase's architecture. Managers ([SessionManager], [TeachingRecorder]) are the only
 * intended callers; screens go through [SessionManager]/[TeachingModeManager], not this directly.
 */
interface TeachingRepository {

    /** Every session known this process, kept in sync with disk on every write below. */
    val sessions: StateFlow<List<TeachingSession>>

    suspend fun saveSession(session: TeachingSession)

    suspend fun updateSession(session: TeachingSession)

    suspend fun loadSession(sessionId: String): TeachingSession?

    suspend fun deleteSession(sessionId: String)

    suspend fun saveTouch(touch: TouchSample)

    /** Writes [imageBytes] to storage and records [frame]'s metadata; [frame.imagePath] is overwritten with the real path. */
    suspend fun saveFrame(frame: ScreenFrame, imageBytes: ByteArray): ScreenFrame

    suspend fun getTouches(sessionId: String): List<TouchSample>

    suspend fun getFrames(sessionId: String): List<ScreenFrame>

    suspend fun getSessions(): List<TeachingSession>
}
