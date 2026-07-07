package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.AIRepository
import com.behaviorengine.core.domain.ai.FailedAttempt
import com.behaviorengine.core.domain.ai.MemoryEngine
import com.behaviorengine.core.domain.ai.SuccessfulRoute
import com.behaviorengine.core.domain.ai.UIVariation
import javax.inject.Inject
import javax.inject.Singleton

private const val NEUTRAL_SUCCESS_RATE = 0.5f

/** Real implementation of [MemoryEngine] — [AIRepository] is the durable store; this adds no separate cache since the repository's file-per-record reads are already cheap at this data scale. */
@Singleton
class MemoryEngineImpl @Inject constructor(
    private val aiRepository: AIRepository
) : MemoryEngine {

    override suspend fun recordSuccess(route: SuccessfulRoute) = aiRepository.saveSuccessfulRoute(route)

    override suspend fun recordFailure(attempt: FailedAttempt) = aiRepository.saveFailedAttempt(attempt)

    override suspend fun recordUiVariation(variation: UIVariation) = aiRepository.saveUiVariation(variation)

    override suspend fun getSuccessRate(workflowId: String, stepIndex: Int): Float {
        val successes = aiRepository.loadSuccessfulRoutes().count { it.workflowId == workflowId && it.stepIndex == stepIndex }
        val failures = aiRepository.loadFailedAttempts().count { it.workflowId == workflowId && it.stepIndex == stepIndex }
        val total = successes + failures
        return if (total == 0) NEUTRAL_SUCCESS_RATE else successes.toFloat() / total
    }

    override suspend fun getFailedAttemptCount(workflowId: String, stepIndex: Int): Int =
        aiRepository.loadFailedAttempts().count { it.workflowId == workflowId && it.stepIndex == stepIndex }

    override suspend fun getAlternateLocations(templateId: String): List<UIVariation> =
        aiRepository.loadUiVariations().filter { it.templateId == templateId }.sortedByDescending { it.recordedAtMillis }

    override suspend fun getPreviousSuccessfulRoute(workflowId: String, stepIndex: Int): SuccessfulRoute? =
        aiRepository.loadSuccessfulRoutes()
            .filter { it.workflowId == workflowId && it.stepIndex == stepIndex }
            .maxByOrNull { it.recordedAtMillis }

    override suspend fun clearCache() = aiRepository.clearCache()
}
