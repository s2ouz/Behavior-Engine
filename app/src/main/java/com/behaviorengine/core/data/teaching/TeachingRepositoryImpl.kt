package com.behaviorengine.core.data.teaching

import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [TeachingRepository] — in-memory only, starting empty every process
 * launch. See the interface's KDoc for why persistence isn't wired up yet.
 */
@Singleton
class TeachingRepositoryImpl @Inject constructor() : TeachingRepository {

    private val _sessions = MutableStateFlow<List<TeachingSession>>(emptyList())
    override val sessions: StateFlow<List<TeachingSession>> = _sessions.asStateFlow()

    override suspend fun saveSession(session: TeachingSession) {
        _sessions.update { current ->
            if (current.any { it.sessionId == session.sessionId }) {
                current.map { if (it.sessionId == session.sessionId) session else it }
            } else {
                current + session
            }
        }
    }

    override suspend fun loadSession(sessionId: String): TeachingSession? =
        _sessions.value.firstOrNull { it.sessionId == sessionId }

    override suspend fun deleteSession(sessionId: String) {
        _sessions.update { current -> current.filterNot { it.sessionId == sessionId } }
    }
}
