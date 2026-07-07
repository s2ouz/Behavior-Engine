package com.behaviorengine.core.domain.matching

/** Everything [ConfidenceEngine] needs for one candidate's final score. */
data class ConfidenceInputs(
    val hashSimilarity: Float,
    val feature: FeatureMatchScore,
    val ocr: OcrMatchScore?,
    val context: Float
)

/**
 * Combines every stage's sub-scores into one 0..100 confidence, per spec's suggested weighting:
 * Visual Features 45% / Shape 20% / OCR 15% / Color 10% / Context 10%. [hashSimilarity] feeds the
 * "Visual Features" bucket alongside [FeatureMatchScore.visualScore]. When [ConfidenceInputs.ocr]
 * is `null` (template has no text), OCR's 15% is redistributed proportionally across the remaining
 * four weights rather than scored 0 — scoring 0 would wrongly punish text-less objects (icons,
 * images) for a signal that was never applicable, which contradicts spec's "ignore OCR" intent.
 */
interface ConfidenceEngine {
    fun computeConfidence(inputs: ConfidenceInputs): Int
}
