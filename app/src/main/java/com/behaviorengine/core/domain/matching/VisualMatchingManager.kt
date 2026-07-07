package com.behaviorengine.core.domain.matching

import android.content.Intent
import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

/**
 * Top-level orchestrator for the Intelligent Visual Matching Engine — "receive automation
 * requests, load the required template, capture the live screen, execute the complete matching
 * pipeline, return the best match," per spec. Runs the pipeline in
 * [com.behaviorengine.core.domain.matching] order: [ScreenAnalyzer] → [CandidateSearchEngine] →
 * [MultiScaleMatcher] → [FeatureMatcher] → [OCRMatcher] → [ContextAnalyzer] → [ConfidenceEngine].
 *
 * This module never clicks or automates anything — [findObject]/[findAllObjects] only ever
 * *locate*. Capturing the live screen reuses the same
 * [com.behaviorengine.core.domain.teaching.ScreenCaptureManager] singleton Teaching Mode uses;
 * since starting a `MediaProjection` needs a foreground service and fresh user consent (Android's
 * requirement, not this project's choice), [createCaptureIntent]/[startCapture] mirror
 * [com.behaviorengine.core.domain.teaching.TeachingModeManager]'s equivalents so this engine can be
 * exercised standalone via the debug screen without Teaching Mode running — but if a capture
 * session (from either feature) is already active, [findObject] reuses it rather than prompting
 * again.
 */
interface VisualMatchingManager {

    val isRunning: StateFlow<Boolean>

    /** True while a `MediaProjection` capture session (this feature's own, or Teaching's) is active. */
    val isCaptureActive: StateFlow<Boolean>

    /** The system consent `Intent` to launch before [startCapture] — skip if [isCaptureActive] is already true. */
    fun createCaptureIntent(): Intent

    fun startCapture(resultCode: Int, data: Intent)

    fun stopCapture()

    /**
     * Finds [templateId] on the current live screen. `null` if no capture session is active, the
     * template doesn't exist, or confidence fell below the 70% "Possible Match" floor. [screen]
     * lets a caller supply its own frame (tests, a future automation engine already holding one)
     * instead of triggering a new capture.
     */
    suspend fun findObject(templateId: String, screen: Bitmap? = null): MatchResult?

    /** Runs [findObject] for every id in [templateIds] (or every known template if `null`) against one shared screen capture. */
    suspend fun findAllObjects(templateIds: List<String>? = null, screen: Bitmap? = null): List<MatchResult>

    /** Cancels whatever [findObject]/[findAllObjects] call is in flight. */
    fun cancel()

    suspend fun clearCache()
}
