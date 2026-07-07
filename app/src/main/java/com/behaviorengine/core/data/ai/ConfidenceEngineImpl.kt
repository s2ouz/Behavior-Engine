package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.AIConfidenceInputs
import com.behaviorengine.core.domain.ai.ConfidenceEngine
import javax.inject.Inject
import javax.inject.Singleton

private const val VISUAL_WEIGHT = 0.35f
private const val WORKFLOW_CONTEXT_WEIGHT = 0.20f
private const val OCR_WEIGHT = 0.15f
private const val HISTORY_WEIGHT = 0.15f
private const val LAYOUT_WEIGHT = 0.10f
private const val TIMING_WEIGHT = 0.05f

/** Real implementation of [ConfidenceEngine] — see that interface's KDoc for the weighting. */
@Singleton
class ConfidenceEngineImpl @Inject constructor() : ConfidenceEngine {

    override fun computeConfidence(inputs: AIConfidenceInputs): Int {
        val weighted = inputs.visualMatch * VISUAL_WEIGHT +
            inputs.workflowContext * WORKFLOW_CONTEXT_WEIGHT +
            inputs.ocrMatch * OCR_WEIGHT +
            inputs.history * HISTORY_WEIGHT +
            inputs.layoutSimilarity * LAYOUT_WEIGHT +
            inputs.timing * TIMING_WEIGHT
        return (weighted * 100).toInt().coerceIn(0, 100)
    }
}
