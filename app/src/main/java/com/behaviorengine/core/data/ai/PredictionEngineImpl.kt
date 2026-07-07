package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.MemoryEngine
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.PredictionEngine
import com.behaviorengine.core.domain.ai.RuntimeContext
import com.behaviorengine.core.domain.ai.Workflow
import javax.inject.Inject
import javax.inject.Singleton

private const val DEFAULT_ESTIMATED_DURATION_MILLIS = 2_000L

/** Real implementation of [PredictionEngine] — the next expected target is literally the workflow's next step; [MemoryEngine] supplies its historical success rate as [Prediction.probability]. */
@Singleton
class PredictionEngineImpl @Inject constructor(
    private val memoryEngine: MemoryEngine
) : PredictionEngine {

    override suspend fun predict(context: RuntimeContext, workflow: Workflow): Prediction {
        val nextStep = workflow.steps.getOrNull(context.currentStep)
            ?: return Prediction(
                expectedScreen = null,
                expectedObjectTemplateId = null,
                expectedOcrText = null,
                estimatedDurationMillis = 0L,
                probability = 1f
            )

        val probability = memoryEngine.getSuccessRate(workflow.id, nextStep.stepIndex)
        return Prediction(
            expectedScreen = null,
            expectedObjectTemplateId = nextStep.templateId,
            expectedOcrText = null,
            estimatedDurationMillis = DEFAULT_ESTIMATED_DURATION_MILLIS,
            probability = probability
        )
    }
}
