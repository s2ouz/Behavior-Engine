package com.behaviorengine.core.domain.objectlearning

import kotlinx.coroutines.flow.StateFlow

/** Business-facing read/write seam over [ObjectLearningStorage] — mirrors [com.behaviorengine.core.domain.teaching.TeachingRepository]'s role. */
interface ObjectRepository {

    /** Every learned object known this process. */
    val objects: StateFlow<List<LearnedObject>>

    suspend fun saveObject(learnedObject: LearnedObject, imageBytes: ByteArray, maskBytes: ByteArray): LearnedObject

    suspend fun loadObject(id: String): LearnedObject?

    suspend fun deleteObject(id: String)

    suspend fun saveTemplate(template: ObjectTemplate)

    suspend fun getTemplates(): List<ObjectTemplate>

    suspend fun getObjects(): List<LearnedObject>

    /** Objects already learned for [sessionId] — lets [ObjectLearningManager] skip touches it already processed, resuming an interrupted run. */
    suspend fun getObjectsForSession(sessionId: String): List<LearnedObject>
}
