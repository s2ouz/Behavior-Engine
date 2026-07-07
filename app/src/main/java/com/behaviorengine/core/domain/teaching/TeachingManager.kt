package com.behaviorengine.core.domain.teaching

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns [TeachingSession] lifecycle transitions — lifecycle only, per this phase's scope: no image
 * capture, no recognition, no screen recording. A future phase's capture engine drives
 * [startSession]/[pauseSession]/[resumeSession] into real work; today they only move
 * [TeachingSession.status] forward so the product's UI and navigation have something real to react
 * to.
 */
interface TeachingManager {

    /** The session currently in flight, or `null` once finished/cancelled/destroyed. */
    val currentSession: StateFlow<TeachingSession?>

    /** Creates a new [TeachingSession] for [objectId], in [TeachingStatus.CREATED]. */
    suspend fun createSession(objectId: String): TeachingSession

    /** Moves [sessionId] to [TeachingStatus.PREPARING]. */
    suspend fun startSession(sessionId: String)

    /** Moves [sessionId] to [TeachingStatus.PAUSED]. */
    suspend fun pauseSession(sessionId: String)

    /** Moves [sessionId] to [TeachingStatus.RUNNING]. */
    suspend fun resumeSession(sessionId: String)

    /** Moves [sessionId] to [TeachingStatus.FINISHED] and clears [currentSession]. */
    suspend fun finishSession(sessionId: String)

    /** Moves [sessionId] to [TeachingStatus.CANCELLED] and clears [currentSession]. */
    suspend fun cancelSession(sessionId: String)

    /** Permanently removes [sessionId] from storage and clears [currentSession] if it matches. */
    suspend fun destroySession(sessionId: String)
}
