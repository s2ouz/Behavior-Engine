package com.behaviorengine.core.domain.matching

/**
 * Matching Rules, per spec: confidence ≥ 85% is a firm [MATCH]; 70–84% is a [POSSIBLE_MATCH],
 * surfaced but not to be auto-trusted by a future automation engine; below 70% is rejected
 * before a [MatchResult] is even created — see [fromConfidence].
 */
enum class MatchQuality {
    MATCH,
    POSSIBLE_MATCH;

    companion object {
        private const val MATCH_THRESHOLD = 85
        private const val POSSIBLE_MATCH_THRESHOLD = 70

        /** `null` means reject — [confidence] fell below [POSSIBLE_MATCH_THRESHOLD]. */
        fun fromConfidence(confidence: Int): MatchQuality? = when {
            confidence >= MATCH_THRESHOLD -> MATCH
            confidence >= POSSIBLE_MATCH_THRESHOLD -> POSSIBLE_MATCH
            else -> null
        }
    }
}
