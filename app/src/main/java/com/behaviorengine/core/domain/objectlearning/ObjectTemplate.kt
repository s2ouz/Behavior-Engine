package com.behaviorengine.core.domain.objectlearning

import kotlinx.serialization.Serializable

/**
 * A reusable visual template — the durable, matchable record [ObjectTemplateManager] produces from
 * one [VisualFeatures] + optional [OcrResult]. Persisted as `feature.json` by
 * [ObjectLearningStorage]. A future phase's template matcher (SPEC-11) reads these back to compare
 * against live screens; nothing in this phase performs that comparison.
 *
 * @param ocrText Empty string when no text was detected — see [OCRManager].
 */
@Serializable
data class ObjectTemplate(
    val id: String,
    val width: Int,
    val height: Int,
    val visualHash: String,
    val cornerFeatureCount: Int,
    val dominantColors: List<Int>,
    val brightness: Float,
    val aspectRatio: Float,
    val edgeDensity: Float,
    val shapeDescriptor: Float,
    val ocrText: String,
    val language: String,
    val ocrConfidence: Float,
    val createdAtMillis: Long
)
