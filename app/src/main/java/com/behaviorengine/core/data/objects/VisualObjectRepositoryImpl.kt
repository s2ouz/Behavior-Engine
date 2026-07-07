package com.behaviorengine.core.data.objects

import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.domain.objects.VisualObjectRepository
import com.behaviorengine.core.domain.objects.VisualObjectStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [VisualObjectRepository] — in-memory only, starting empty every
 * process launch. See the interface's KDoc for why persistence isn't wired up yet.
 */
@Singleton
class VisualObjectRepositoryImpl @Inject constructor() : VisualObjectRepository {

    private val _objects = MutableStateFlow<List<VisualObject>>(emptyList())
    override val objects: StateFlow<List<VisualObject>> = _objects.asStateFlow()

    override suspend fun createObject(name: String): VisualObject {
        val now = System.currentTimeMillis()
        val created = VisualObject(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAtMillis = now,
            lastModifiedMillis = now,
            status = VisualObjectStatus.READY
        )
        _objects.update { it + created }
        return created
    }

    override suspend fun updateObject(updated: VisualObject) {
        _objects.update { current ->
            current.map { existing ->
                if (existing.id == updated.id) updated.copy(lastModifiedMillis = System.currentTimeMillis()) else existing
            }
        }
    }

    override suspend fun deleteObject(objectId: String) {
        _objects.update { current -> current.filterNot { it.id == objectId } }
    }

    override suspend fun loadObjects(): List<VisualObject> = _objects.value

    override fun searchObjects(query: String): List<VisualObject> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return _objects.value
        return _objects.value.filter { it.name.contains(trimmed, ignoreCase = true) }
    }
}
