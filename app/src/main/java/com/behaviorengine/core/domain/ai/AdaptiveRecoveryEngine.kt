package com.behaviorengine.core.domain.ai

/**
 * Invoked by [AIDecisionManager] when [DecisionEngine]'s confidence is too low to trust directly.
 * Tries the spec's recovery ladder in order — alternative object (via [MemoryEngine]'s recorded
 * [UIVariation]s), alternative scroll, previous successful route, wait-and-retry, search the
 * entire screen, a generic fallback, then abort — returning whichever step first produces a usable
 * [Decision]. Always terminates in a real [Decision] (worst case [DecisionAction.STOP_EXECUTION]),
 * never throws, per spec's "always prefer safe termination over unsafe execution."
 */
interface AdaptiveRecoveryEngine {
    suspend fun recover(context: RuntimeContext, workflow: Workflow, failedDecision: Decision): Decision
}
