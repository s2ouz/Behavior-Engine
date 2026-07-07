package com.behaviorengine.core.domain.objectlearning

import kotlinx.coroutines.flow.StateFlow

/**
 * Top-level orchestrator — "receive completed teaching sessions, process every touch, select the
 * closest frame, detect the touched object, save the learned object." Coordinates
 * [FrameSelectionManager], [ObjectDetectionManager], [CropManager], [FeatureExtractionManager],
 * [OCRManager], [ObjectTemplateManager], and [ObjectRepository] behind one seam, exactly how
 * [com.behaviorengine.core.domain.teaching.TeachingModeManager] coordinates the teaching module's
 * own managers.
 *
 * [isRunning]/[progress] are `StateFlow`s rather than the spec's literal `isRunning()` function,
 * matching this project's established StateFlow-driven UI convention (see
 * [com.behaviorengine.core.domain.teaching.TeachingModeManager]'s KDoc for the same adaptation).
 *
 * Resuming an interrupted session needs no separate progress file: [processSession] simply skips
 * any touch that [ObjectRepository.getObjectsForSession] already has a [LearnedObject] for.
 */
interface ObjectLearningManager {

    val isRunning: StateFlow<Boolean>

    val progress: StateFlow<LearningProgress?>

    /** Launches [processSession] on a background scope; returns immediately. */
    fun startLearning(sessionId: String)

    /** Requests a graceful stop after the touch currently being processed finishes — already-learned objects are kept. */
    fun stopLearning()

    /** Processes every touch in [sessionId]'s recording, per the pipeline in [ObjectDetectionManager]'s KDoc. Safe to call directly (e.g. from a test) without [startLearning]. */
    suspend fun processSession(sessionId: String)

    /** Runs the full per-touch pipeline once; returns `null` if the touch was rejected by a quality rule. */
    suspend fun processTouch(sessionId: String, touch: com.behaviorengine.core.domain.teaching.TouchSample): LearnedObject?

    /** Aborts immediately, discarding the touch currently being processed (already-saved objects are kept). */
    fun cancel()
}
