package com.behaviorengine.core.domain.ai

/** Raw file I/O for AADE data — its own `AI/` root, sibling to `Matching/`/`Teaching/`. */
interface AIStorage {
    suspend fun writeDecision(decision: Decision)
    suspend fun listDecisions(): List<Decision>
    suspend fun writePrediction(prediction: Prediction)
    suspend fun writeRuntimeStatistics(statistics: AIRuntimeStatistics)
    suspend fun listRuntimeStatistics(): List<AIRuntimeStatistics>
    suspend fun writeSuccessfulRoute(route: SuccessfulRoute)
    suspend fun listSuccessfulRoutes(): List<SuccessfulRoute>
    suspend fun writeFailedAttempt(attempt: FailedAttempt)
    suspend fun listFailedAttempts(): List<FailedAttempt>
    suspend fun writeUiVariation(variation: UIVariation)
    suspend fun listUiVariations(): List<UIVariation>
    suspend fun writeWorkflow(workflow: Workflow)
    suspend fun readWorkflow(id: String): Workflow?
    suspend fun listWorkflowIds(): List<String>
}
