package com.behaviorengine.core.data.objectlearning

import android.content.Context
import com.behaviorengine.core.domain.objectlearning.LearnedObject
import com.behaviorengine.core.domain.objectlearning.ObjectLearningStorage
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [ObjectLearningStorage]. Shares the same `Teaching/` root as
 * [com.behaviorengine.core.data.teaching.TeachingStorageImpl] (each computes it independently,
 * since the two modules otherwise have no reason to depend on each other) but owns its own
 * `Objects/`, `Templates/`, `Masks/`, `Features/` subfolders, per spec.
 */
@Singleton
class ObjectLearningStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ObjectLearningStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val rootDir: File by lazy {
        File(context.getExternalFilesDir(null), "Teaching").apply { mkdirs() }
    }
    private val objectsDir: File by lazy { File(rootDir, "Objects").apply { mkdirs() } }
    private val templatesDir: File by lazy { File(rootDir, "Templates").apply { mkdirs() } }
    private val masksDir: File by lazy { File(rootDir, "Masks").apply { mkdirs() } }
    private val featuresDir: File by lazy { File(rootDir, "Features").apply { mkdirs() } }

    override suspend fun writeObjectImage(learnedObjectId: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val file = File(objectsDir, "$learnedObjectId.webp")
            file.writeBytes(bytes)
            file.absolutePath
        }

    override suspend fun writeMask(learnedObjectId: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val file = File(masksDir, "$learnedObjectId.png")
            file.writeBytes(bytes)
            file.absolutePath
        }

    override suspend fun writeTemplate(template: ObjectTemplate) {
        withContext(Dispatchers.IO) {
            val file = File(templatesDir, "${template.id}.json")
            file.writeText(json.encodeToString(ObjectTemplate.serializer(), template))
            // A copy under Features/ as feature.json-per-template, matching the spec's literal
            // storage layout, distinct from Templates/ which future phases may use for something else.
            File(featuresDir, "${template.id}.json").writeText(
                json.encodeToString(ObjectTemplate.serializer(), template)
            )
        }
    }

    override suspend fun readTemplate(templateId: String): ObjectTemplate? = withContext(Dispatchers.IO) {
        val file = File(templatesDir, "$templateId.json")
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString(ObjectTemplate.serializer(), file.readText()) }.getOrNull()
    }

    override suspend fun listTemplateIds(): List<String> = withContext(Dispatchers.IO) {
        templatesDir.listFiles { file -> file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    override suspend fun writeLearnedObject(learnedObject: LearnedObject) {
        withContext(Dispatchers.IO) {
            File(objectsDir, "${learnedObject.id}.json")
                .writeText(json.encodeToString(LearnedObject.serializer(), learnedObject))
        }
    }

    override suspend fun readLearnedObject(id: String): LearnedObject? = withContext(Dispatchers.IO) {
        val file = File(objectsDir, "$id.json")
        if (!file.exists()) return@withContext null
        runCatching { json.decodeFromString(LearnedObject.serializer(), file.readText()) }.getOrNull()
    }

    override suspend fun listLearnedObjectIds(): List<String> = withContext(Dispatchers.IO) {
        objectsDir.listFiles { file -> file.extension == "json" }
            ?.map { it.nameWithoutExtension }
            ?: emptyList()
    }

    override suspend fun deleteObject(learnedObjectId: String) {
        withContext(Dispatchers.IO) {
            File(objectsDir, "$learnedObjectId.webp").delete()
            File(objectsDir, "$learnedObjectId.json").delete()
            File(masksDir, "$learnedObjectId.png").delete()
        }
    }
}
