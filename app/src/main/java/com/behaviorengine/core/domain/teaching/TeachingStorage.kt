package com.behaviorengine.core.domain.teaching

import java.io.File

/**
 * The bottom layer of "UI → Manager → Repository → Storage → JSON Database": raw file I/O only,
 * no business logic. Everything lives under the app's own external files directory (no
 * `MANAGE_EXTERNAL_STORAGE` needed, scoped-storage compliant), mirroring the spec's requested
 * `Android/data/<package>/Teaching/{Sessions,Frames,Json,Temp}` layout — see
 * [com.behaviorengine.core.data.teaching.TeachingStorageImpl] for the exact folder mapping.
 */
interface TeachingStorage {

    /** Writes [document] as `session.json`, replacing any existing file for the same session. */
    suspend fun writeSessionDocument(document: TeachingSessionDocument)

    /** Reads back a previously-written session document, or `null` if none exists for [sessionId]. */
    suspend fun readSessionDocument(sessionId: String): TeachingSessionDocument?

    /** Every session id currently on disk. */
    suspend fun listSessionIds(): List<String>

    /** Deletes a session's JSON and every frame image that belongs to it. */
    suspend fun deleteSession(sessionId: String)

    /** Writes one WEBP-encoded frame for [sessionId] and returns the absolute path it was written to. */
    suspend fun writeFrame(sessionId: String, fileName: String, bytes: ByteArray): String

    /** Total bytes currently used by all teaching data — backs the UI's "Storage Used" stat. */
    fun storageUsedBytes(): Long

    /** The frames directory for [sessionId], creating it if necessary. Exposed for [File]-based cleanup only. */
    fun framesDirectory(sessionId: String): File
}
