package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.ActionScore
import com.behaviorengine.core.domain.ai.DecisionAction
import com.behaviorengine.core.domain.ai.MemoryEngine
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.ReasoningEngine
import com.behaviorengine.core.domain.ai.ReasoningResult
import com.behaviorengine.core.domain.ai.RuntimeContext
import com.behaviorengine.core.domain.ai.ScreenType
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.matching.MatchResult
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [ReasoningEngine]. Deliberately doesn't re-derive OCR/visual confidence
 * from raw pixels — [MatchResult.confidence] (from [com.behaviorengine.core.domain.matching.VisualMatchingManager])
 * already blends visual/shape/OCR/color/context per its own weighting, so [visualMatch] reuses it
 * directly rather than duplicating that computation.
 */
@Singleton
class ReasoningEngineImpl @Inject constructor(
    private val memoryEngine: MemoryEngine,
    private val objectRepository: ObjectRepository
) : ReasoningEngine {

    override suspend fun reason(context: RuntimeContext, workflow: Workflow, prediction: Prediction): ReasoningResult {
        val expectedTemplateId = prediction.expectedObjectTemplateId
        val matchedResult = context.detectedObjects.firstOrNull { it.templateId == expectedTemplateId }

        val visualMatch = (matchedResult?.confidence ?: 0) / 100f

        val workflowContext = when {
            expectedTemplateId == null -> 1f
            matchedResult != null -> 1f
            context.screenType == ScreenType.ERROR || context.screenType == ScreenType.UNKNOWN -> 0f
            else -> 0.5f
        }

        val expectedTemplate = expectedTemplateId?.let { id -> objectRepository.getTemplates().firstOrNull { it.id == id } }
        val ocrMatch = when {
            expectedTemplate == null || expectedTemplate.ocrText.isBlank() -> 0.5f
            context.ocrText.contains(expectedTemplate.ocrText, ignoreCase = true) -> 1f
            else -> 0.1f
        }

        val history = memoryEngine.getSuccessRate(workflow.id, context.currentStep)

        val knownTemplateCount = workflow.steps.map { it.templateId }.distinct().size.coerceAtLeast(1)
        val layoutSimilarity = (context.detectedObjects.size.toFloat() / knownTemplateCount).coerceIn(0f, 1f)

        val failedAttempts = memoryEngine.getFailedAttemptCount(workflow.id, context.currentStep)
        val timing = (1f / (1f + failedAttempts)).coerceIn(0f, 1f)

        return ReasoningResult(
            actionScores = buildActionScores(context, matchedResult, visualMatch),
            visualMatch = visualMatch,
            workflowContext = workflowContext,
            ocrMatch = ocrMatch,
            history = history,
            layoutSimilarity = layoutSimilarity,
            timing = timing
        )
    }

    private fun buildActionScores(context: RuntimeContext, matchedResult: MatchResult?, visualMatch: Float): List<ActionScore> = listOf(
        ActionScore(DecisionAction.CONTINUE_WORKFLOW, visualMatch, "Target object visual match confidence"),
        ActionScore(DecisionAction.RETRY_STEP, (1f - visualMatch) * 0.5f, "Target not confidently found yet"),
        ActionScore(DecisionAction.SEARCH_ALTERNATIVE_OBJECT, if (matchedResult == null) 0.4f else 0.1f, "Target missing from its expected location"),
        ActionScore(DecisionAction.SCROLL, if (matchedResult == null && context.screenType != ScreenType.LOADING) 0.3f else 0.05f, "Target may be off-screen"),
        ActionScore(DecisionAction.WAIT, if (context.screenType == ScreenType.LOADING) 0.8f else 0.05f, "Screen still loading"),
        ActionScore(
            DecisionAction.ASK_USER_CONFIRMATION,
            if (context.screenType == ScreenType.PERMISSION_DIALOG || context.screenType == ScreenType.CONFIRMATION_DIALOG) 0.9f else 0f,
            "Dialog requires a human decision"
        )
    )
}
