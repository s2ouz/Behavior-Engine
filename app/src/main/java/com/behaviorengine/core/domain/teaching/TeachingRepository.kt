package com.behaviorengine.core.domain.teaching

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns teaching session storage. [com.behaviorengine.core.data.teaching.TeachingRepositoryImpl]
 * is in-memory only this phase, matching
 * [com.behaviorengine.core.domain.objects.VisualObjectRepository]'s reasoning — there's no capture
 * data yet to make persistence meaningful. [TeachingManager] is the only intended caller; screens
 * go through the manager, not this repository directly.
 */
interface TeachingRepository {

    /** Every session created so far this process, in no particular guaranteed order — this list IS the session history. */
    val sessions: StateFlow<List<TeachingSession>>

    /** Inserts [session], or replaces the stored session sharing its [TeachingSession.sessionId]. */
    suspend fun saveSession(session: TeachingSession)

    /** One-shot fetch of a single session by id, or `null` if it isn't found. */
    suspend fun loadSession(sessionId: String): TeachingSession?

    /** Removes the session identified by [sessionId]; a no-op if it isn't found. */
    suspend fun deleteSession(sessionId: String)
}
