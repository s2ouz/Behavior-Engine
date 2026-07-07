package com.behaviorengine.core.domain.ai

import com.behaviorengine.core.domain.matching.MatchResult

/**
 * [StateRecognitionEngine]'s output for one live screen capture. Transient — [ContextManager]
 * folds the parts worth remembering into [RuntimeContext]; this itself is never persisted.
 */
data class ScreenState(
    val screenType: ScreenType,
    val confidence: Int,
    val detectedObjects: List<MatchResult>,
    val detectedText: String
)
