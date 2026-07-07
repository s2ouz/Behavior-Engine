package com.behaviorengine.core.domain.ai

import kotlinx.serialization.Serializable

/** One action [DecisionEngine] considered but didn't choose — the "alternative actions considered" explainability requirement. */
@Serializable
data class DecisionAlternative(
    val action: DecisionAction,
    val confidence: Int,
    val reason: String
)

/**
 * [DecisionEngine]'s output — deliberately carries its own explanation ([reason], [alternatives])
 * rather than requiring a caller to reconstruct "why" later, per spec's Explainability section:
 * "every AI decision must include why this action was chosen, confidence, alternatives considered,
 * expected result."
 */
@Serializable
data class Decision(
    val decisionId: String,
    val action: DecisionAction,
    val reason: String,
    val confidence: Int,
    val expectedOutcome: String,
    val fallbackStrategy: DecisionAction?,
    val alternatives: List<DecisionAlternative>,
    val timestamp: Long
)
