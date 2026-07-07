package com.behaviorengine.core.domain.objects

import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the taught-object library. [com.behaviorengine.core.data.objects.VisualObjectRepositoryImpl]
 * is in-memory only this phase — "prepare repository architecture" per spec, not "persist it,"
 * and there is no image data yet to make persistence meaningful. A future phase backing this
 * with Room only has to change that one class; every consumer already goes through this interface.
 *
 * Functions are `suspend` even though today's in-memory implementation completes instantly —
 * matching the same forward-looking choice made for
 * [com.behaviorengine.core.domain.profile.UserProfileRepository], so adding real I/O later never
 * requires changing call sites.
 */
interface VisualObjectRepository {

    /** Every object currently known, in no particular guaranteed order. */
    val objects: StateFlow<List<VisualObject>>

    /** Creates and stores a new object named [name], defaulting to [VisualObjectStatus.READY]. */
    suspend fun createObject(name: String): VisualObject

    /** Replaces the stored object sharing [VisualObject.id] with [updated], stamping [VisualObject.lastModifiedMillis]. */
    suspend fun updateObject(updated: VisualObject)

    /** Removes the object identified by [objectId]; a no-op if it isn't found. */
    suspend fun deleteObject(objectId: String)

    /** One-shot fetch of the current library — the seam a future real data source hooks into. */
    suspend fun loadObjects(): List<VisualObject>

    /** Pure, synchronous, case-insensitive name filter over the currently-held objects. */
    fun searchObjects(query: String): List<VisualObject>
}
