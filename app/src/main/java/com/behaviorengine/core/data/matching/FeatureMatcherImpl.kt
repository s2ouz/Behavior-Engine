package com.behaviorengine.core.data.matching

import android.graphics.Bitmap
import android.graphics.Color
import com.behaviorengine.core.domain.matching.FeatureMatchScore
import com.behaviorengine.core.domain.matching.FeatureMatcher
import com.behaviorengine.core.domain.objectlearning.BoundingBox
import com.behaviorengine.core.domain.objectlearning.FeatureExtractionManager
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

private const val VISUAL_HASH_WEIGHT = 0.5f
private const val VISUAL_EDGE_WEIGHT = 0.3f
private const val VISUAL_CORNER_WEIGHT = 0.2f

/**
 * Real implementation of [FeatureMatcher]. Reuses
 * [FeatureExtractionManager] to extract the candidate's own
 * [com.behaviorengine.core.domain.objectlearning.VisualFeatures] — with a synthetic fully-opaque
 * mask, since a live screen crop has no segmentation mask the way a taught object does — then
 * diffs field-by-field against [ObjectTemplate]'s stored measurements.
 */
@Singleton
class FeatureMatcherImpl @Inject constructor(
    private val featureExtractionManager: FeatureExtractionManager
) : FeatureMatcher {

    override suspend fun score(template: ObjectTemplate, candidate: Bitmap): FeatureMatchScore {
        val mask = Bitmap.createBitmap(candidate.width, candidate.height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val boundingBox = BoundingBox(0, 0, candidate.width, candidate.height)
        val features = featureExtractionManager.extractFeatures(candidate, mask, boundingBox)
        mask.recycle()

        val hashSimilarity = hammingSimilarity(features.visualHash, template.visualHash)
        val edgeSimilarity = (1f - abs(features.edgeDensity - template.edgeDensity)).coerceIn(0f, 1f)
        val cornerRange = max(features.cornerFeatureCount, template.cornerFeatureCount).coerceAtLeast(1)
        val cornerSimilarity = (1f - abs(features.cornerFeatureCount - template.cornerFeatureCount).toFloat() / cornerRange).coerceIn(0f, 1f)
        val visualScore = hashSimilarity * VISUAL_HASH_WEIGHT + edgeSimilarity * VISUAL_EDGE_WEIGHT + cornerSimilarity * VISUAL_CORNER_WEIGHT

        val aspectRange = max(features.aspectRatio, template.aspectRatio).coerceAtLeast(0.01f)
        val aspectSimilarity = (1f - abs(features.aspectRatio - template.aspectRatio) / aspectRange).coerceIn(0f, 1f)
        val shapeSimilarity = (1f - abs(features.shapeDescriptor - template.shapeDescriptor)).coerceIn(0f, 1f)
        val shapeScore = (aspectSimilarity + shapeSimilarity) / 2f

        val colorScore = colorPaletteSimilarity(features.dominantColors, template.dominantColors)

        return FeatureMatchScore(visualScore = visualScore, shapeScore = shapeScore, colorScore = colorScore)
    }

    private fun colorPaletteSimilarity(candidateColors: List<Int>, templateColors: List<Int>): Float {
        if (candidateColors.isEmpty() || templateColors.isEmpty()) return 0f
        return candidateColors.map { closestColorSimilarity(it, templateColors) }.average().toFloat()
    }
}
