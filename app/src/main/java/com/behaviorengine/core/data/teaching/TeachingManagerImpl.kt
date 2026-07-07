package com.behaviorengine.core.data.teaching

import com.behaviorengine.core.domain.teaching.TeachingManager
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [TeachingManager]. Every transition writes through to [repository] first
 * so [TeachingRepository.sessions] (the session history) always reflects the latest status, then
 * updates [currentSession] if the transitioned session is the one currently in flight.
 */
@Singleton
class TeachingManagerImpl @Inject constructor(
    private val repository: TeachingRepository
) : TeachingManager {

    private val _currentSession = MutableStateFlow<TeachingSession?>(null)
    override val currentSession: StateFlow<TeachingSession?> = _currentSession.asStateFlow()

    override suspend fun createSession(objectId: String): TeachingSession {
        val session = TeachingSession(
            sessionId = UUID.randomUUID().toString(),
            objectId = objectId,
            createdAtMillis = System.currentTimeMillis(),
            status = TeachingStatus.CREATED
        )
        repository.saveSession(session)
        _currentSession.value = session
        return session
    }

    override suspend fun startSession(sessionId: String) {
        transition(sessionId, TeachingStatus.PREPARING)
    }

    override suspend fun pauseSession(sessionId: String) {
        transition(sessionId, TeachingStatus.PAUSED)
    }

    override suspend fun resumeSession(sessionId: String) {
        transition(sessionId, TeachingStatus.RUNNING)
    }

    override suspend fun finishSession(sessionId: String) {
        transition(sessionId, TeachingStatus.FINISHED)
        clearCurrentSessionIfMatches(sessionId)
    }

    override suspend fun cancelSession(sessionId: String) {
        transition(sessionId, TeachingStatus.CANCELLED)
        clearCurrentSessionIfMatches(sessionId)
    }

    override suspend fun destroySession(sessionId: String) {
        repository.deleteSession(sessionId)
        clearCurrentSessionIfMatches(sessionId)
    }

    private suspend fun transition(sessionId: String, status: TeachingStatus) {
        val existing = requireNotNull(repository.loadSession(sessionId)) {
            "No teaching session with id $sessionId"
        }
        val updated = existing.copy(status = status)
        repository.saveSession(updated)
        if (_currentSession.value?.sessionId == sessionId) {
            _currentSession.value = updated
        }
    }

    private fun clearCurrentSessionIfMatches(sessionId: String) {
        if (_currentSession.value?.sessionId == sessionId) {
            _currentSession.value = null
        }
    }
}
