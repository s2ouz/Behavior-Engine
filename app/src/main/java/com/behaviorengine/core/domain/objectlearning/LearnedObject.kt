package com.behaviorengine.core.domain.objectlearning

import kotlinx.serialization.Serializable

/**
 * One successfully learned object — the link between a specific recorded touch and the
 * [ObjectTemplate] learned from it. `objectPath`/`maskPath` are absolute file paths, matching how
 * [com.behaviorengine.core.domain.teaching.ScreenFrame.imagePath] handles image storage — pixels
 * live on disk, never round-tripped through this model.
 */
@Serializable
data class LearnedObject(
    val id: String,
    val sessionId: String,
    val touchId: String,
    val frameId: String,
    val templateId: String,
    val objectPath: String,
    val maskPath: String,
    val createdAtMillis: Long,
    val confidence: Float
)
