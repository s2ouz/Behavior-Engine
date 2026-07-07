package com.behaviorengine.core.domain.ai

import com.behaviorengine.core.domain.matching.MatchResult

/** [ContextManager]'s output — everything [DecisionEngine]/[ReasoningEngine] need about "where things stand right now," per spec. */
data class RuntimeContext(
    val workflowId: String,
    val currentStep: Int,
    val screenType: ScreenType,
    val activePackage: String,
    val detectedObjects: List<MatchResult>,
    val ocrText: String,
    val variables: Map<String, String>,
    val timestamp: Long
)
