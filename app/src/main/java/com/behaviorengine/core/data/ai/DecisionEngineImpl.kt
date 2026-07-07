package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.AIConfidenceInputs
import com.behaviorengine.core.domain.ai.ConfidenceEngine
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.DecisionAction
import com.behaviorengine.core.domain.ai.DecisionAlternative
import com.behaviorengine.core.domain.ai.DecisionEngine
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.ReasoningResult
import com.behaviorengine.core.domain.ai.RuntimeContext
import com.behaviorengine.core.domain.ai.ScreenType
import com.behaviorengine.core.domain.ai.Workflow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val CONTINUE_THRESHOLD = 70
private const val MAX_ALTERNATIVES = 3

/**
 * Real implementation of [DecisionEngine]. Ranks [ReasoningResult.actionScores] and picks the
 * top-scoring action, overridden only by two hard rules that don't belong in a fuzzy score: a
 * finished workflow always stops, and a recognized permission/confirmation dialog always asks the
 * user rather than guessing. Every [Decision] carries its own [Decision.reason]/[Decision.alternatives]/
 * [Decision.expectedOutcome] per spec's Explainability requirement — nothing here is a black box.
 */
@Singleton
class DecisionEngineImpl @Inject constructor(
    private val confidenceEngine: ConfidenceEngine
) : DecisionEngine {

    override fun decide(context: RuntimeContext, workflow: Workflow, prediction: Prediction, reasoning: ReasoningResult): Decision {
        val confidence = confidenceEngine.computeConfidence(
            AIConfidenceInputs(
                visualMatch = reasoning.visualMatch,
                workflowContext = reasoning.workflowContext,
                ocrMatch = reasoning.ocrMatch,
                history = reasoning.history,
                layoutSimilarity = reasoning.layoutSimilarity,
                timing = reasoning.timing
            )
        )

        val stepExists = workflow.steps.getOrNull(context.currentStep) != null
        val ranked = reasoning.actionScores.sortedByDescending { it.score }
        val best = ranked.firstOrNull()

        val action = when {
            !stepExists -> DecisionAction.STOP_EXECUTION
            context.screenType == ScreenType.PERMISSION_DIALOG || context.screenType == ScreenType.CONFIRMATION_DIALOG -> DecisionAction.ASK_USER_CONFIRMATION
            best != null && best.action == DecisionAction.CONTINUE_WORKFLOW && confidence < CONTINUE_THRESHOLD -> DecisionAction.RETRY_STEP
            best != null -> best.action
            else -> DecisionAction.STOP_EXECUTION
        }

        val alternatives = ranked.filterNot { it.action == action }.take(MAX_ALTERNATIVES)
            .map { DecisionAlternative(it.action, (it.score * 100).toInt(), it.reason) }

        return Decision(
            decisionId = UUID.randomUUID().toString(),
            action = action,
            reason = reasonFor(action, context, confidence, stepExists),
            confidence = confidence,
            expectedOutcome = expectedOutcomeFor(action, workflow, context),
            fallbackStrategy = fallbackFor(action),
            alternatives = alternatives,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun reasonFor(action: DecisionAction, context: RuntimeContext, confidence: Int, stepExists: Boolean): String = when (action) {
        DecisionAction.CONTINUE_WORKFLOW -> "Target object for step ${context.currentStep} found with $confidence% decision confidence"
        DecisionAction.ASK_USER_CONFIRMATION -> "Screen recognized as ${context.screenType}, which needs a human decision"
        DecisionAction.WAIT -> "Screen appears to still be loading"
        DecisionAction.SCROLL -> "Target not visible in the current viewport"
        DecisionAction.SEARCH_ALTERNATIVE_OBJECT -> "Target missing from its taught location; searching known variations"
        DecisionAction.RETRY_STEP -> "Decision confidence ($confidence%) below the $CONTINUE_THRESHOLD% continue threshold"
        DecisionAction.GO_BACK -> "Unrecognized screen; attempting to return to a known state"
        DecisionAction.RESTART_WORKFLOW -> "Repeated failures at this step; restarting from the beginning"
        DecisionAction.STOP_EXECUTION -> if (!stepExists) "Workflow complete — no steps remaining" else "Confidence too low to safely continue ($confidence%)"
    }

    private fun expectedOutcomeFor(action: DecisionAction, workflow: Workflow, context: RuntimeContext): String = when (action) {
        DecisionAction.CONTINUE_WORKFLOW -> "Advance to step ${context.currentStep + 1} of ${workflow.steps.size}"
        DecisionAction.RETRY_STEP -> "Re-evaluate step ${context.currentStep} on the next capture"
        DecisionAction.SEARCH_ALTERNATIVE_OBJECT -> "Locate step ${context.currentStep}'s target at an alternate position"
        DecisionAction.SCROLL -> "Reveal the target by scrolling, then re-evaluate"
        DecisionAction.WAIT -> "Screen finishes loading before the next evaluation"
        DecisionAction.GO_BACK -> "Return to a screen the workflow recognizes"
        DecisionAction.RESTART_WORKFLOW -> "Resume from step 0 with a clean context"
        DecisionAction.STOP_EXECUTION -> "Execution halts; no further steps are attempted"
        DecisionAction.ASK_USER_CONFIRMATION -> "User resolves the dialog before execution resumes"
    }

    private fun fallbackFor(action: DecisionAction): DecisionAction? = when (action) {
        DecisionAction.CONTINUE_WORKFLOW -> DecisionAction.RETRY_STEP
        DecisionAction.RETRY_STEP -> DecisionAction.SEARCH_ALTERNATIVE_OBJECT
        DecisionAction.SEARCH_ALTERNATIVE_OBJECT -> DecisionAction.SCROLL
        DecisionAction.SCROLL -> DecisionAction.WAIT
        DecisionAction.WAIT -> DecisionAction.RETRY_STEP
        DecisionAction.GO_BACK -> DecisionAction.RESTART_WORKFLOW
        DecisionAction.RESTART_WORKFLOW -> DecisionAction.STOP_EXECUTION
        DecisionAction.ASK_USER_CONFIRMATION -> DecisionAction.STOP_EXECUTION
        DecisionAction.STOP_EXECUTION -> null
    }
}
