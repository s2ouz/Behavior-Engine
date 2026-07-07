package com.behaviorengine.core.data.teaching

import com.behaviorengine.core.domain.teaching.ScreenFrame
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingSessionDocument
import com.behaviorengine.core.domain.teaching.TeachingStorage
import com.behaviorengine.core.domain.teaching.TouchSample
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [TeachingRepository]. Holds each open session's touches/frames metadata
 * in memory (cheap — metadata only, never pixel bytes, see [ScreenFrame]) and writes the full
 * [TeachingSessionDocument] through to [TeachingStorage] after every mutation. A single [mutex]
 * guards all of it: session counts here are small enough that a global lock is simpler and just
 * as fast as per-session locks, and correctness (touches/frames arrive from different coroutines)
 * matters far more than shaving microseconds off a JSON write.
 */
@Singleton
class TeachingRepositoryImpl @Inject constructor(
    private val storage: TeachingStorage
) : TeachingRepository {

    private val mutex = Mutex()
    private val touchesBySession = mutableMapOf<String, MutableList<TouchSample>>()
    private val framesBySession = mutableMapOf<String, MutableList<ScreenFrame>>()

    private val _sessions = MutableStateFlow<List<TeachingSession>>(emptyList())
    override val sessions: StateFlow<List<TeachingSession>> = _sessions.asStateFlow()

    override suspend fun saveSession(session: TeachingSession) {
        mutex.withLock {
            touchesBySession.getOrPut(session.id) { mutableListOf() }
            framesBySession.getOrPut(session.id) { mutableListOf() }
            persistLocked(session)
        }
        _sessions.update { current -> current.filterNot { it.id == session.id } + session }
    }

    override suspend fun updateSession(session: TeachingSession) {
        mutex.withLock { persistLocked(session) }
        _sessions.update { current -> current.map { if (it.id == session.id) session else it } }
    }

    override suspend fun loadSession(sessionId: String): TeachingSession? =
        _sessions.value.firstOrNull { it.id == sessionId } ?: storage.readSessionDocument(sessionId)?.session

    override suspend fun deleteSession(sessionId: String) {
        mutex.withLock {
            touchesBySession.remove(sessionId)
            framesBySession.remove(sessionId)
        }
        storage.deleteSession(sessionId)
        _sessions.update { current -> current.filterNot { it.id == sessionId } }
    }

    override suspend fun saveTouch(touch: TouchSample) {
        mutex.withLock {
            touchesBySession.getOrPut(touch.sessionId) { mutableListOf() }.add(touch)
            currentSessionLocked(touch.sessionId)?.let { persistLocked(it) }
        }
    }

    override suspend fun saveFrame(frame: ScreenFrame, imageBytes: ByteArray): ScreenFrame {
        val fileName = "frame_%05d.webp".format(frame.frameNumber)
        val path = storage.writeFrame(frame.sessionId, fileName, imageBytes)
        val stored = frame.copy(imagePath = path)
        mutex.withLock {
            framesBySession.getOrPut(frame.sessionId) { mutableListOf() }.add(stored)
            currentSessionLocked(frame.sessionId)?.let { persistLocked(it) }
        }
        return stored
    }

    override suspend fun getTouches(sessionId: String): List<TouchSample> =
        mutex.withLock { touchesBySession[sessionId]?.toList() } ?: emptyList()

    override suspend fun getFrames(sessionId: String): List<ScreenFrame> =
        mutex.withLock { framesBySession[sessionId]?.toList() } ?: emptyList()

    override suspend fun getSessions(): List<TeachingSession> {
        val onDisk = storage.listSessionIds().mapNotNull { storage.readSessionDocument(it)?.session }
        _sessions.value = onDisk
        return onDisk
    }

    private fun currentSessionLocked(sessionId: String): TeachingSession? =
        _sessions.value.firstOrNull { it.id == sessionId }

    private suspend fun persistLocked(session: TeachingSession) {
        val document = TeachingSessionDocument(
            session = session,
            touches = touchesBySession[session.id]?.toList() ?: emptyList(),
            frames = framesBySession[session.id]?.toList() ?: emptyList()
        )
        storage.writeSessionDocument(document)
    }
}
