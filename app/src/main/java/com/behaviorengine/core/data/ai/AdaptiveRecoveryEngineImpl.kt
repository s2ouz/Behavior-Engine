package com.behaviorengine.core.data.ai

import com.behaviorengine.core.common.AIDecisionLogger
import com.behaviorengine.core.domain.ai.AdaptiveRecoveryEngine
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.DecisionAction
import com.behaviorengine.core.domain.ai.MemoryEngine
import com.behaviorengine.core.domain.ai.RuntimeContext
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.matching.VisualMatchingManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [AdaptiveRecoveryEngine] — see that interface's KDoc for the ladder and its "always terminates" guarantee. */
@Singleton
class AdaptiveRecoveryEngineImpl @Inject constructor(
    private val visualMatchingManager: VisualMatchingManager,
    private val memoryEngine: MemoryEngine,
    private val logger: AIDecisionLogger
) : AdaptiveRecoveryEngine {

    override suspend fun recover(context: RuntimeContext, workflow: Workflow, failedDecision: Decision): Decision {
        val step = workflow.steps.getOrNull(context.currentStep)
            ?: return terminal(DecisionAction.STOP_EXECUTION, "Workflow already complete")

        // 1. Alternative Object: a fresh search in case the first attempt was a transient miss.
        val retried = runCatching { visualMatchingManager.findObject(step.templateId) }.getOrNull()
        logger.recovery("Alternative Object", retried != null)
        if (retried != null) {
            return terminal(DecisionAction.CONTINUE_WORKFLOW, "Recovery found the target on retry (${retried.confidence}%)", retried.confidence)
        }

        // 2. Alternative Scroll: propose scrolling to reveal the target — execution stays a no-op this phase.
        val hasKnownVariation = memoryEngine.getAlternateLocations(step.templateId).isNotEmpty()
        logger.recovery("Alternative Scroll", hasKnownVariation)
        if (hasKnownVariation) {
            return terminal(DecisionAction.SCROLL, "Target has moved before; scrolling to search known alternate positions")
        }

        // 3. Previous Successful Route: trust history if this exact step has succeeded before.
        val previousRoute = memoryEngine.getPreviousSuccessfulRoute(workflow.id, context.currentStep)
        logger.recovery("Previous Successful Route", previousRoute != null)
        if (previousRoute != null) {
            return terminal(DecisionAction.CONTINUE_WORKFLOW, "A previous run completed this step successfully", previousRoute.confidence)
        }

        // 4. Wait and Retry: the screen may simply not have finished loading yet.
        logger.recovery("Wait and Retry", true)
        if (context.screenType.name == "LOADING") {
            return terminal(DecisionAction.WAIT, "Screen still loading; waiting before retrying")
        }

        // 5. Search Entire Screen: broaden the search to every known template, not just this workflow's.
        val broadSearch = runCatching { visualMatchingManager.findAllObjects(templateIds = null) }.getOrDefault(emptyList())
        val foundElsewhere = broadSearch.firstOrNull { it.templateId == step.templateId }
        logger.recovery("Search Entire Screen", foundElsewhere != null)
        if (foundElsewhere != null) {
            return terminal(DecisionAction.SEARCH_ALTERNATIVE_OBJECT, "Target found during a full-screen search", foundElsewhere.confidence)
        }

        // 6. Fallback Strategy: the screen doesn't match anything this workflow recognizes.
        logger.recovery("Fallback Strategy", false)
        if (context.currentStep > 0) {
            return terminal(DecisionAction.GO_BACK, "Attempting to return to a recognized screen")
        }

        // 7. Abort: nothing worked and there's nowhere safe to fall back to.
        logger.recovery("Abort", false)
        return terminal(DecisionAction.STOP_EXECUTION, "All recovery strategies exhausted; stopping safely")
    }

    private fun terminal(action: DecisionAction, reason: String, confidence: Int = 0): Decision = Decision(
        decisionId = UUID.randomUUID().toString(),
        action = action,
        reason = reason,
        confidence = confidence,
        expectedOutcome = "Recovery-driven: $action",
        fallbackStrategy = if (action == DecisionAction.STOP_EXECUTION) null else DecisionAction.STOP_EXECUTION,
        alternatives = emptyList(),
        timestamp = System.currentTimeMillis()
    )
}
