package com.behaviorengine.core.data.matching

import android.graphics.Bitmap
import android.graphics.Color
import com.behaviorengine.core.domain.matching.AnalyzedScreen
import com.behaviorengine.core.domain.matching.CandidateRegion
import com.behaviorengine.core.domain.matching.MultiScaleMatcher
import com.behaviorengine.core.domain.matching.ScaleMatchResult
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import javax.inject.Inject
import javax.inject.Singleton

private const val HASH_WIDTH = 9
private const val HASH_HEIGHT = 8
private const val MIN_SAMPLE_DIMENSION_PX = 4

/**
 * Real implementation of [MultiScaleMatcher]. For each scale, samples a
 * `templateWidth*scale x templateHeight*scale` window from the live screen centered on the
 * candidate, resizes it to the template's exact size, and compares its perceptual hash (same
 * dHash algorithm as
 * [com.behaviorengine.core.data.objectlearning.FeatureExtractionManagerImpl.differenceHash],
 * intentionally duplicated in miniature here rather than shared across modules — see that class's
 * KDoc for this project's general preference for small, self-contained pixel algorithms over
 * cross-module coupling) against [ObjectTemplate.visualHash]. Cheap enough to run at all 11
 * scales per candidate; only the winning scale is handed to [com.behaviorengine.core.domain.matching.FeatureMatcher]/
 * [com.behaviorengine.core.domain.matching.OCRMatcher] for the expensive full comparison.
 */
@Singleton
class MultiScaleMatcherImpl @Inject constructor() : MultiScaleMatcher {

    override suspend fun matchScale(template: ObjectTemplate, region: CandidateRegion, screen: AnalyzedScreen): ScaleMatchResult? {
        var best: ScaleMatchResult? = null
        for (scale in MultiScaleMatcher.SCALE_LEVELS) {
            val sampleWidth = (template.width * scale).toInt().coerceAtLeast(1).coerceAtMost(screen.width)
            val sampleHeight = (template.height * scale).toInt().coerceAtLeast(1).coerceAtMost(screen.height)
            val left = (region.centerX - sampleWidth / 2f).toInt().coerceIn(0, (screen.width - sampleWidth).coerceAtLeast(0))
            val top = (region.centerY - sampleHeight / 2f).toInt().coerceIn(0, (screen.height - sampleHeight).coerceAtLeast(0))
            val width = sampleWidth.coerceAtMost(screen.width - left)
            val height = sampleHeight.coerceAtMost(screen.height - top)
            if (width < MIN_SAMPLE_DIMENSION_PX || height < MIN_SAMPLE_DIMENSION_PX) continue

            val sample = runCatching { Bitmap.createBitmap(screen.bitmap, left, top, width, height) }.getOrNull() ?: continue
            val resized = Bitmap.createScaledBitmap(sample, template.width.coerceAtLeast(1), template.height.coerceAtLeast(1), true)
            if (sample !== resized && sample !== screen.bitmap) sample.recycle()

            val similarity = hammingSimilarity(differenceHash(resized), template.visualHash)
            if (resized !== screen.bitmap) resized.recycle()

            if (best == null || similarity > best.hashSimilarity) {
                best = ScaleMatchResult(
                    region = region,
                    scale = scale,
                    hashSimilarity = similarity,
                    sampledLeft = left,
                    sampledTop = top,
                    sampledWidth = width,
                    sampledHeight = height
                )
            }
        }
        return best
    }

    private fun differenceHash(bitmap: Bitmap): String {
        val small = Bitmap.createScaledBitmap(bitmap, HASH_WIDTH, HASH_HEIGHT, true)
        var hash = 0L
        var bitIndex = 0
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                val left = luminance(small.getPixel(x, y))
                val right = luminance(small.getPixel(x + 1, y))
                if (left > right) hash = hash or (1L shl bitIndex)
                bitIndex++
            }
        }
        if (small !== bitmap) small.recycle()
        return hash.toULong().toString(16).padStart(16, '0')
    }

    private fun luminance(pixel: Int): Int =
        (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000
}
