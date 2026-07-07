package com.behaviorengine.core.data.objectlearning

import com.behaviorengine.core.domain.objectlearning.LearnedObject
import com.behaviorengine.core.domain.objectlearning.ObjectLearningStorage
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectRepositoryImpl @Inject constructor(
    private val storage: ObjectLearningStorage
) : ObjectRepository {

    private val _objects = MutableStateFlow<List<LearnedObject>>(emptyList())
    override val objects: StateFlow<List<LearnedObject>> = _objects.asStateFlow()

    override suspend fun saveObject(learnedObject: LearnedObject, imageBytes: ByteArray, maskBytes: ByteArray): LearnedObject {
        val imagePath = storage.writeObjectImage(learnedObject.id, imageBytes)
        val maskPath = storage.writeMask(learnedObject.id, maskBytes)
        val stored = learnedObject.copy(objectPath = imagePath, maskPath = maskPath)
        storage.writeLearnedObject(stored)
        _objects.update { it + stored }
        return stored
    }

    override suspend fun loadObject(id: String): LearnedObject? =
        _objects.value.firstOrNull { it.id == id } ?: storage.readLearnedObject(id)

    override suspend fun deleteObject(id: String) {
        storage.deleteObject(id)
        _objects.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun saveTemplate(template: ObjectTemplate) {
        storage.writeTemplate(template)
    }

    override suspend fun getTemplates(): List<ObjectTemplate> =
        storage.listTemplateIds().mapNotNull { storage.readTemplate(it) }

    override suspend fun getObjects(): List<LearnedObject> {
        val onDisk = storage.listLearnedObjectIds().mapNotNull { storage.readLearnedObject(it) }
        _objects.value = onDisk
        return onDisk
    }

    override suspend fun getObjectsForSession(sessionId: String): List<LearnedObject> =
        getObjects().filter { it.sessionId == sessionId }
}
