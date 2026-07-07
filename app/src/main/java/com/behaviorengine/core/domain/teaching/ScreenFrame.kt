package com.behaviorengine.core.domain.teaching

import kotlinx.serialization.Serializable

/**
 * Metadata for one captured screen frame. Deliberately holds only [imagePath] — never the pixel
 * data itself — so a session with hundreds of frames stays cheap to keep in memory as a
 * [TeachingSession]'s frame list; the actual WEBP bytes are written straight to disk by
 * [TeachingStorage] and never round-trip back through this model.
 */
@Serializable
data class ScreenFrame(
    val id: String,
    val sessionId: String,
    val frameNumber: Int,
    val timestampMillis: Long,
    val imagePath: String,
    val width: Int,
    val height: Int,
    val rotation: Int
)
