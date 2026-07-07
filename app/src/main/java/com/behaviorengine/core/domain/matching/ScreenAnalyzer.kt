package com.behaviorengine.core.domain.matching

import android.graphics.Bitmap

/**
 * A captured screen plus its precomputed grayscale — shared by [CandidateSearchEngine] and
 * [MultiScaleMatcher] so grayscale conversion happens exactly once per search, not once per
 * candidate region.
 */
data class AnalyzedScreen(
    val bitmap: Bitmap,
    val grayscale: IntArray,
    val width: Int,
    val height: Int
)

/**
 * Prepares a raw captured frame for matching: contrast normalization and light noise reduction,
 * plus the grayscale precompute every later stage reuses. Rotation is already normalized upstream —
 * [com.behaviorengine.core.domain.teaching.ScreenCaptureManager] always captures at the display's
 * current orientation — so this stage doesn't re-derive it. Color/theme normalization (light vs.
 * dark UI) isn't a separate step here: every downstream comparison (hash, dominant color, edge
 * density) already operates on relative pixel structure rather than absolute brightness, which is
 * what actually makes matching theme-tolerant, per the spec's "handle light/dark themes when
 * possible" goal.
 */
interface ScreenAnalyzer {
    fun analyze(raw: Bitmap): AnalyzedScreen
}
