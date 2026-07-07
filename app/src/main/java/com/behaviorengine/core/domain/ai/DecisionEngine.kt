package com.behaviorengine.core.domain.ai

/** Evaluates [ReasoningResult]'s candidate actions and commits to one [Decision], with full explainability. */
interface DecisionEngine {
    fun decide(context: RuntimeContext, workflow: Workflow, prediction: Prediction, reasoning: ReasoningResult): Decision
}
