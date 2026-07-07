package com.behaviorengine.core.domain.ai

/**
 * Records outcomes so future runs of the same [Workflow] get better — "improve future
 * executions," per spec. Backed by [AIRepository] for durability; also serves fast in-session
 * lookups [AdaptiveRecoveryEngine] needs mid-run.
 */
interface MemoryEngine {
    suspend fun recordSuccess(route: SuccessfulRoute)
    suspend fun recordFailure(attempt: FailedAttempt)
    suspend fun recordUiVariation(variation: UIVariation)

    /** 0..1 historical success rate for this workflow/step, or a neutral 0.5 with no history yet. */
    suspend fun getSuccessRate(workflowId: String, stepIndex: Int): Float

    /** How many [FailedAttempt]s are on record for this workflow/step — feeds [ReasoningEngine]'s timing signal. */
    suspend fun getFailedAttemptCount(workflowId: String, stepIndex: Int): Int

    /** Previously-observed alternate on-screen locations for [templateId], newest first. */
    suspend fun getAlternateLocations(templateId: String): List<UIVariation>

    suspend fun getPreviousSuccessfulRoute(workflowId: String, stepIndex: Int): SuccessfulRoute?

    suspend fun clearCache()
}
