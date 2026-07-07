package com.behaviorengine.core.domain.matching

import android.graphics.Bitmap
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate

/**
 * Sub-scores (0..1 each) feeding [ConfidenceEngine]. Stands in for the spec's separate "ORB
 * descriptors / edge maps / shape similarity / dominant colors / visual hash / aspect ratio" list:
 * [visualScore] combines hash + edge-density + corner-count similarity, [shapeScore] combines
 * aspect-ratio + shape-descriptor similarity, [colorScore] is dominant-color similarity — the same
 * three-way split [ConfidenceEngine]'s weighting (Visual/Shape/Color) expects.
 */
data class FeatureMatchScore(
    val visualScore: Float,
    val shapeScore: Float,
    val colorScore: Float
)

/**
 * Compares one already-scaled candidate crop against [ObjectTemplate]'s stored measurements —
 * reuses [com.behaviorengine.core.domain.objectlearning.FeatureExtractionManager] to extract the
 * candidate's own [com.behaviorengine.core.domain.objectlearning.VisualFeatures] first (with a
 * synthetic fully-opaque mask, since the live screen has no segmentation mask the way a taught
 * object crop does), then diffs field-by-field against the template.
 */
interface FeatureMatcher {
    /** [candidate] must already be resized to [ObjectTemplate.width]x[ObjectTemplate.height], as [MultiScaleMatcher] produces. */
    suspend fun score(template: ObjectTemplate, candidate: Bitmap): FeatureMatchScore
}
