package com.behaviorengine.core.domain.ai

import kotlinx.serialization.Serializable

/** [PredictionEngine]'s output — what's expected to happen next, before [DecisionEngine] commits to an action. */
@Serializable
data class Prediction(
    val expectedScreen: ScreenType?,
    val expectedObjectTemplateId: String?,
    val expectedOcrText: String?,
    val estimatedDurationMillis: Long,
    val probability: Float
)
