package com.behaviorengine.core.domain.ai

import kotlinx.serialization.Serializable

/** [MemoryEngine] record: one step's target was found and a [Decision] to act on it was made. */
@Serializable
data class SuccessfulRoute(
    val id: String,
    val workflowId: String,
    val stepIndex: Int,
    val templateId: String,
    val confidence: Int,
    val recordedAtMillis: Long
)

/** [MemoryEngine] record: one step's target could not be resolved into a usable decision. */
@Serializable
data class FailedAttempt(
    val id: String,
    val workflowId: String,
    val stepIndex: Int,
    val reason: String,
    val recordedAtMillis: Long
)

/**
 * [MemoryEngine] record: a template was found at a screen position other than where it was
 * taught — feeds [AdaptiveRecoveryEngine]'s "Alternative Object" / "Alternative Scroll" recovery
 * attempts on future runs.
 */
@Serializable
data class UIVariation(
    val id: String,
    val templateId: String,
    val screenPositionX: Float,
    val screenPositionY: Float,
    val recordedAtMillis: Long
)

/** One [AIDecisionManager] run's aggregate performance, for [AIRepository.saveRuntime]. */
@Serializable
data class AIRuntimeStatistics(
    val id: String,
    val workflowId: String,
    val stepsCompleted: Int,
    val stepsFailed: Int,
    val recoveriesUsed: Int,
    val averageDecisionLatencyMillis: Long,
    val recordedAtMillis: Long
)
