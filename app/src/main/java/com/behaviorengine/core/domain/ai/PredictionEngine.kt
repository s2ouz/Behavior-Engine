package com.behaviorengine.core.domain.ai

/** Predicts what should appear next — the workflow's next step's target — before [DecisionEngine] commits to an action. */
interface PredictionEngine {
    suspend fun predict(context: RuntimeContext, workflow: Workflow): Prediction
}
