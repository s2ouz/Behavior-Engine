package com.behaviorengine.core.domain.ai

import android.graphics.Bitmap

/**
 * Classifies a live screen capture into a [ScreenType], reusing
 * [com.behaviorengine.core.domain.matching.VisualMatchingManager] (for [ScreenState.detectedObjects])
 * and [com.behaviorengine.core.domain.objectlearning.OCRManager] (for [ScreenState.detectedText])
 * rather than a trained scene classifier this project has no way to bundle — see
 * [com.behaviorengine.core.data.ai.StateRecognitionEngineImpl] for the honest, keyword/heuristic
 * classification this implies.
 */
interface StateRecognitionEngine {
    suspend fun recognize(screen: Bitmap, workflow: Workflow): ScreenState
}
