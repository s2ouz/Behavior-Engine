package com.behaviorengine.core.domain.ai

/**
 * The seam [AIDecisionManager] calls to actually carry out a [Decision] — taps, scrolls, back
 * presses. **No real Automation Engine exists in this codebase yet**: SPEC-13 is written as the
 * intelligence layer "above the Automation Engine," but no prior phase built one, and this spec
 * requests no new execution permission (`AccessibilityService` with `FLAG_REQUEST_MOTION_EVENTS`,
 * or similar). Rather than fabricate unauthorized device control, this interface is real and
 * complete, but [com.behaviorengine.core.data.ai.NoOpAutomationExecutor] is the only implementation
 * this phase — it computes and logs the decision without physically acting on the device, exactly
 * matching spec's own "always prefer safe termination over unsafe execution." A future phase can
 * bind a real executor here without touching [AIDecisionManager] or anything upstream.
 */
interface AutomationExecutor {
    suspend fun execute(decision: Decision, context: RuntimeContext): ExecutionResult
}
