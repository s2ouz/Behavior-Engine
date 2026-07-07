package com.behaviorengine.core.domain.objectlearning

import android.graphics.Bitmap

/**
 * Locates the UI element a touch was aimed at within a screen frame, trying each [DetectionMethod]
 * in priority order until one yields a usable candidate. See
 * [com.behaviorengine.objectlearning.ObjectDetectionManagerImpl] for the real detector chain and
 * why real OpenCV/ORB weren't used for the contour/edge tiers.
 */
interface ObjectDetectionManager {

    /** Runs every detection tier and returns the single best [DetectionCandidate] for the touch at ([touchX], [touchY]) in [frame]. */
    suspend fun detectObject(frame: Bitmap, touchX: Float, touchY: Float): DetectionCandidate

    /** Runs every tier and returns every candidate produced, best-effort — used by [selectBestCandidate]. */
    suspend fun generateCandidates(frame: Bitmap, touchX: Float, touchY: Float): List<DetectionCandidate>

    /** Picks the highest-confidence candidate from [candidates], or the lowest-confidence fallback if all are weak. */
    fun selectBestCandidate(candidates: List<DetectionCandidate>): DetectionCandidate?
}
