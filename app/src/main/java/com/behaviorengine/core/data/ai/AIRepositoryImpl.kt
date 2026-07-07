package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.AIRepository
import com.behaviorengine.core.domain.ai.AIRuntimeStatistics
import com.behaviorengine.core.domain.ai.AIStorage
import com.behaviorengine.core.domain.ai.Decision
import com.behaviorengine.core.domain.ai.FailedAttempt
import com.behaviorengine.core.domain.ai.Prediction
import com.behaviorengine.core.domain.ai.SuccessfulRoute
import com.behaviorengine.core.domain.ai.UIVariation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AIRepositoryImpl @Inject constructor(
    private val storage: AIStorage
) : AIRepository {

    override suspend fun saveDecision(decision: Decision) = storage.writeDecision(decision)

    override suspend fun loadHistory(): List<Decision> = storage.listDecisions()

    override suspend fun savePrediction(prediction: Prediction) = storage.writePrediction(prediction)

    override suspend fun saveRuntime(statistics: AIRuntimeStatistics) = storage.writeRuntimeStatistics(statistics)

    override suspend fun loadRuntimeStatistics(): List<AIRuntimeStatistics> = storage.listRuntimeStatistics()

    override suspend fun saveSuccessfulRoute(route: SuccessfulRoute) = storage.writeSuccessfulRoute(route)

    override suspend fun loadSuccessfulRoutes(): List<SuccessfulRoute> = storage.listSuccessfulRoutes()

    override suspend fun saveFailedAttempt(attempt: FailedAttempt) = storage.writeFailedAttempt(attempt)

    override suspend fun loadFailedAttempts(): List<FailedAttempt> = storage.listFailedAttempts()

    override suspend fun saveUiVariation(variation: UIVariation) = storage.writeUiVariation(variation)

    override suspend fun loadUiVariations(): List<UIVariation> = storage.listUiVariations()

    override suspend fun clearCache() {
        // Durable history is intentionally not wiped by cache-clear — see MatchingCache's
        // equivalent KDoc for the same "cache vs. durable history" distinction. Nothing to do here;
        // this exists so callers have one place to route a future "clear AI history" action through.
    }
}
