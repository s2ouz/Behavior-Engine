package com.behaviorengine.core.domain.ai

/**
 * Combines [ReasoningEngine]'s six signals into one 0..100 decision confidence, per spec's
 * weighting: Visual Match 35% / Workflow Context 20% / OCR Match 15% / History 15% / Layout
 * Similarity 10% / Timing 5%. Distinct from [com.behaviorengine.core.domain.matching.ConfidenceEngine] —
 * that one scores "is this the right pixels," this one scores "is this the right *decision*,"
 * a strictly higher-level judgment that also weighs workflow history and timing.
 */
interface ConfidenceEngine {
    fun computeConfidence(inputs: AIConfidenceInputs): Int
}
