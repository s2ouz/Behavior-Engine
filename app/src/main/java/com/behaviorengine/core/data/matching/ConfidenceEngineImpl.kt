package com.behaviorengine.core.data.matching

import com.behaviorengine.core.domain.matching.ConfidenceEngine
import com.behaviorengine.core.domain.matching.ConfidenceInputs
import javax.inject.Inject
import javax.inject.Singleton

private const val VISUAL_WEIGHT = 0.45f
private const val SHAPE_WEIGHT = 0.20f
private const val OCR_WEIGHT = 0.15f
private const val COLOR_WEIGHT = 0.10f
private const val CONTEXT_WEIGHT = 0.10f

/** Real implementation of [ConfidenceEngine] — see that interface's KDoc for the weighting and the OCR-redistribution rule. */
@Singleton
class ConfidenceEngineImpl @Inject constructor() : ConfidenceEngine {

    override fun computeConfidence(inputs: ConfidenceInputs): Int {
        val visualScore = (inputs.hashSimilarity + inputs.feature.visualScore) / 2f
        val ocr = inputs.ocr

        val weighted = if (ocr != null) {
            visualScore * VISUAL_WEIGHT +
                inputs.feature.shapeScore * SHAPE_WEIGHT +
                ocr.textSimilarity * OCR_WEIGHT +
                inputs.feature.colorScore * COLOR_WEIGHT +
                inputs.context * CONTEXT_WEIGHT
        } else {
            val remaining = 1f - OCR_WEIGHT
            visualScore * (VISUAL_WEIGHT / remaining) +
                inputs.feature.shapeScore * (SHAPE_WEIGHT / remaining) +
                inputs.feature.colorScore * (COLOR_WEIGHT / remaining) +
                inputs.context * (CONTEXT_WEIGHT / remaining)
        }

        return (weighted * 100).toInt().coerceIn(0, 100)
    }
}
