package com.behaviorengine.core.domain.matching

/**
 * One region of the live screen worth running [MultiScaleMatcher] against — [CandidateSearchEngine]'s
 * output, reducing the search space instead of scanning every pixel. [priority] encodes which
 * search method proposed it (cache hit ≈100, OCR-text region ≈80, edge/color heuristics ≈40-60)
 * so higher-confidence sources get evaluated first — see [CandidateSearchEngine] for the exact scale.
 */
data class CandidateRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val priority: Int,
    val score: Float
) {
    val centerX: Float get() = x + width / 2f
    val centerY: Float get() = y + height / 2f
}
