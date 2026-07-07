package com.behaviorengine.core.data.teaching

import android.content.Context
import com.behaviorengine.core.domain.teaching.TeachingSessionDocument
import com.behaviorengine.core.domain.teaching.TeachingStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [TeachingStorage]. Everything lives under
 * `context.getExternalFilesDir(null)/Teaching/` — scoped-storage compliant, no
 * `MANAGE_EXTERNAL_STORAGE` needed — mirroring the spec's requested `Android/data/<package>/Teaching/`
 * location as closely as a modern, non-rooted Android app is allowed to.
 *
 * `Sessions/<id>/session.json` holds each session's full document (session + touches + frames);
 * `Frames/<id>/` holds that session's WEBP images; `Json/` is reserved for a future
 * cross-session index (e.g. a session list cache) not needed yet since [sessions] already lists
 * every session by scanning `Sessions/`; `Temp/` is scratch space for the atomic
 * write-then-rename below, so a process death mid-write can never leave a half-written
 * `session.json` behind — directly the "session corruption" failure mode the spec calls out.
 */
@Singleton
class TeachingStorageImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : TeachingStorage {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val rootDir: File by lazy {
        File(context.getExternalFilesDir(null), "Teaching").apply { mkdirs() }
    }
    private val sessionsDir: File by lazy { File(rootDir, "Sessions").apply { mkdirs() } }
    private val framesDir: File by lazy { File(rootDir, "Frames").apply { mkdirs() } }
    private val jsonDir: File by lazy { File(rootDir, "Json").apply { mkdirs() } }
    private val tempDir: File by lazy { File(rootDir, "Temp").apply { mkdirs() } }

    init {
        // Force creation of every documented subfolder up front, including the currently-unused
        // Json/ index folder, so the on-disk layout always matches what this phase's spec asks for.
        jsonDir.mkdirs()
    }

    override suspend fun writeSessionDocument(document: TeachingSessionDocument) {
        withContext(Dispatchers.IO) {
            val sessionDir = sessionDirectory(document.session.id).apply { mkdirs() }
            val target = File(sessionDir, SESSION_FILE_NAME)
            val temp = File(tempDir, "${document.session.id}_$SESSION_FILE_NAME.tmp")
            temp.writeText(json.encodeToString(TeachingSessionDocument.serializer(), document))
            temp.copyTo(target, overwrite = true)
            temp.delete()
        }
    }

    override suspend fun readSessionDocument(sessionId: String): TeachingSessionDocument? =
        withContext(Dispatchers.IO) {
            val file = File(sessionDirectory(sessionId), SESSION_FILE_NAME)
            if (!file.exists()) return@withContext null
            runCatching {
                json.decodeFromString(TeachingSessionDocument.serializer(), file.readText())
            }.getOrNull()
        }

    override suspend fun listSessionIds(): List<String> = withContext(Dispatchers.IO) {
        sessionsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
    }

    override suspend fun deleteSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            sessionDirectory(sessionId).deleteRecursively()
            framesDirectory(sessionId).deleteRecursively()
        }
    }

    override suspend fun writeFrame(sessionId: String, fileName: String, bytes: ByteArray): String =
        withContext(Dispatchers.IO) {
            val dir = framesDirectory(sessionId).apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            file.absolutePath
        }

    override fun storageUsedBytes(): Long = rootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    override fun framesDirectory(sessionId: String): File = File(framesDir, sessionId)

    private fun sessionDirectory(sessionId: String): File = File(sessionsDir, sessionId)

    private companion object {
        const val SESSION_FILE_NAME = "session.json"
    }
}
