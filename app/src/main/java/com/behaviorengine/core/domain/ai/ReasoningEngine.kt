package com.behaviorengine.core.domain.ai

/**
 * Combines object confidence, OCR confidence, workflow history, screen similarity, and previous
 * successful paths (per spec) into per-action [ActionScore]s plus the six raw signals
 * [ConfidenceEngine] weighs — the "why" behind whichever action [DecisionEngine] ultimately picks.
 */
interface ReasoningEngine {
    suspend fun reason(context: RuntimeContext, workflow: Workflow, prediction: Prediction): ReasoningResult
}
