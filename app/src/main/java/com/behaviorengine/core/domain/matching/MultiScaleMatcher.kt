package com.behaviorengine.core.domain.matching

import com.behaviorengine.core.domain.objectlearning.ObjectTemplate

/**
 * The winning scale for one [CandidateRegion], from a cheap perceptual-hash pass across every
 * [MultiScaleMatcher.SCALE_LEVELS]. [sampledLeft]/[sampledTop]/[sampledWidth]/[sampledHeight]
 * describe the screen-space rectangle that scale sampled — callers re-crop it from the live
 * screen to feed [FeatureMatcher]/[OCRMatcher], rather than this result carrying a [android.graphics.Bitmap]
 * itself, keeping this a plain data model.
 */
data class ScaleMatchResult(
    val region: CandidateRegion,
    val scale: Float,
    val hashSimilarity: Float,
    val sampledLeft: Int,
    val sampledTop: Int,
    val sampledWidth: Int,
    val sampledHeight: Int
)

/**
 * Searches one [CandidateRegion] at every scale in [SCALE_LEVELS]. "Scale" here means the size of
 * the screen-space window sampled around the candidate's center before it's resized down/up to
 * [ObjectTemplate.width]x[ObjectTemplate.height] for comparison — not a resize of the template
 * itself — so a template taught at one size can still be found larger or smaller on a different
 * screen/resolution/density.
 */
interface MultiScaleMatcher {
    /** `null` if [region] falls entirely outside [screen] at every scale. */
    suspend fun matchScale(template: ObjectTemplate, region: CandidateRegion, screen: AnalyzedScreen): ScaleMatchResult?

    companion object {
        /** Required scale levels, per spec. */
        val SCALE_LEVELS = listOf(0.50f, 0.60f, 0.70f, 0.80f, 0.90f, 1.00f, 1.10f, 1.25f, 1.50f, 1.75f, 2.00f)
    }
}
