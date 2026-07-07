package com.behaviorengine.core.domain.matching

import com.behaviorengine.core.domain.objectlearning.BoundingBox
import kotlinx.serialization.Serializable

/**
 * IVME's final output — the best-scoring, quality-gated location of one [ObjectTemplate][com.behaviorengine.core.domain.objectlearning.ObjectTemplate]
 * on the live screen. [centerX]/[centerY] are what a future automation engine would act on;
 * everything else here is provenance for the debug UI and [MatchingRepository] history.
 */
@Serializable
data class MatchResult(
    val id: String,
    val templateId: String,
    val confidence: Int,
    val boundingBox: BoundingBox,
    val centerX: Float,
    val centerY: Float,
    val width: Int,
    val height: Int,
    val rotation: Float,
    val scale: Float,
    val method: String,
    val quality: MatchQuality,
    val foundAtMillis: Long
)
