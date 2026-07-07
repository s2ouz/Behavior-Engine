package com.behaviorengine.core.domain.objectlearning

/** Live state for the "Processing Session..." UI — see [ObjectLearningManager.progress]. */
data class LearningProgress(
    val sessionId: String,
    val currentTouchIndex: Int,
    val totalTouches: Int,
    val objectsLearned: Int,
    val currentConfidence: Float,
    val estimatedRemainingMillis: Long
)
