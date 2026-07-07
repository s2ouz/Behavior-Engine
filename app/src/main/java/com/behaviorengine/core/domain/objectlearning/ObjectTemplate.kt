package com.behaviorengine.core.domain.objectlearning

import kotlinx.serialization.Serializable

/**
 * A reusable visual template — the durable, matchable record [ObjectTemplateManager] produces from
 * one [VisualFeatures] + optional [OcrResult]. Persisted as `feature.json` by
 * [ObjectLearningStorage]. SPEC-11's `com.behaviorengine.core.domain.matching.VisualMatchingManager`
 * reads these back to compare against live screens.
 *
 * @param ocrText Empty string when no text was detected — see [OCRManager].
 * @param screenPositionX Normalized (0..1) horizontal center of this object within the frame it
 * was taught on, i.e. `centerX / frameWidth`. `-1f` for templates learned before this field
 * existed — a sentinel `matching`'s `ContextAnalyzer` treats as "no positional context," not
 * zero/top-left.
 * @param screenPositionY Same as [screenPositionX], vertically.
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
    val createdAtMillis: Long,
    val screenPositionX: Float = -1f,
    val screenPositionY: Float = -1f
)
