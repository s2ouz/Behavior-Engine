package com.behaviorengine.core.data.ai

import com.behaviorengine.core.common.AIDecisionLogger
import com.behaviorengine.core.domain.ai.AutomationExecutor
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.DecisionAction
import com.behaviorengine.core.domain.ai.ExecutionResult
import com.behaviorengine.core.domain.ai.RuntimeContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [AutomationExecutor] this phase — see that interface's KDoc for why no real device
 * control exists yet. [STOP_EXECUTION]/[ASK_USER_CONFIRMATION] never "execute" anything by
 * definition, so those report success trivially; every other action reports success with a
 * message making clear nothing was physically performed, so the dashboard and [AIRepository]
 * history are never mistaken for records of real device interaction.
 */
@Singleton
class NoOpAutomationExecutor @Inject constructor(
    private val logger: AIDecisionLogger
) : AutomationExecutor {

    override suspend fun execute(decision: Decision, context: RuntimeContext): ExecutionResult {
        val message = when (decision.action) {
            DecisionAction.STOP_EXECUTION, DecisionAction.ASK_USER_CONFIRMATION ->
                "${decision.action} requires no device action"
            else ->
                "Decision computed (${decision.action}) but not physically executed — no AutomationExecutor is wired up yet"
        }
        logger.executionSuccess(decision.action.name)
        return ExecutionResult(success = true, message = message, executedAtMillis = System.currentTimeMillis())
    }
}
