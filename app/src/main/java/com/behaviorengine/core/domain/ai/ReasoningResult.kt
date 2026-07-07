package com.behaviorengine.core.domain.ai

/** [ReasoningEngine]'s score (0..1) for one candidate action, with its own explanation. */
data class ActionScore(
    val action: DecisionAction,
    val score: Float,
    val reason: String
)

/**
 * [ReasoningEngine]'s output — per-action candidate scores plus the six raw signals (0..1 each)
 * that [ConfidenceEngine] combines into a single 0..100 confidence for whichever action
 * [DecisionEngine] ultimately picks.
 */
data class ReasoningResult(
    val actionScores: List<ActionScore>,
    val visualMatch: Float,
    val workflowContext: Float,
    val ocrMatch: Float,
    val history: Float,
    val layoutSimilarity: Float,
    val timing: Float
)

/** [ConfidenceEngine]'s input — the same six signals, named for the weighting table in its KDoc. */
data class AIConfidenceInputs(
    val visualMatch: Float,
    val workflowContext: Float,
    val ocrMatch: Float,
    val history: Float,
    val layoutSimilarity: Float,
    val timing: Float
)
