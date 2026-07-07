package com.behaviorengine.core.domain.objectlearning

/**
 * Raw file I/O for learned objects — sibling to
 * [com.behaviorengine.core.domain.teaching.TeachingStorage], sharing the same `Teaching/` root but
 * owning its own `Objects/`, `Templates/`, `Masks/`, and `Features/` subfolders, per spec.
 */
interface ObjectLearningStorage {

    /** Writes [bytes] (WEBP, 90% quality) as this object's image and returns the absolute path. */
    suspend fun writeObjectImage(learnedObjectId: String, bytes: ByteArray): String

    /** Writes [bytes] (PNG, lossless) as this object's binary mask and returns the absolute path. */
    suspend fun writeMask(learnedObjectId: String, bytes: ByteArray): String

    /** Writes [template] as `feature.json`. */
    suspend fun writeTemplate(template: ObjectTemplate)

    suspend fun readTemplate(templateId: String): ObjectTemplate?

    suspend fun listTemplateIds(): List<String>

    /** Writes [learnedObject]'s metadata (everything except the image/mask bytes) alongside its image file. */
    suspend fun writeLearnedObject(learnedObject: LearnedObject)

    suspend fun readLearnedObject(id: String): LearnedObject?

    suspend fun listLearnedObjectIds(): List<String>

    suspend fun deleteObject(learnedObjectId: String)
}
