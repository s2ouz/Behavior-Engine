package com.behaviorengine.core.domain.ai

/** Business-facing read/write seam over [com.behaviorengine.core.data.ai.AIStorage] — mirrors [com.behaviorengine.core.domain.matching.MatchingRepository]'s role. */
interface AIRepository {
    suspend fun saveDecision(decision: Decision)
    suspend fun loadHistory(): List<Decision>
    suspend fun savePrediction(prediction: Prediction)
    suspend fun saveRuntime(statistics: AIRuntimeStatistics)
    suspend fun loadRuntimeStatistics(): List<AIRuntimeStatistics>
    suspend fun saveSuccessfulRoute(route: SuccessfulRoute)
    suspend fun loadSuccessfulRoutes(): List<SuccessfulRoute>
    suspend fun saveFailedAttempt(attempt: FailedAttempt)
    suspend fun loadFailedAttempts(): List<FailedAttempt>
    suspend fun saveUiVariation(variation: UIVariation)
    suspend fun loadUiVariations(): List<UIVariation>
    suspend fun clearCache()
}
