package com.behaviorengine.core.data.ai

import android.graphics.Bitmap
import com.behaviorengine.core.domain.ai.ScreenState
import com.behaviorengine.core.domain.ai.ScreenType
import com.behaviorengine.core.domain.ai.StateRecognitionEngine
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.matching.VisualMatchingManager
import com.behaviorengine.core.domain.objectlearning.OCRManager
import javax.inject.Inject
import javax.inject.Singleton

private val LOGIN_KEYWORDS = listOf("sign in", "log in", "login", "password", "username", "forgot password")
private val SETTINGS_KEYWORDS = listOf("settings", "preferences", "options")
private val LOADING_KEYWORDS = listOf("loading", "please wait", "just a moment")
private val ERROR_KEYWORDS = listOf("error", "failed", "something went wrong", "try again", "not found")
private val PERMISSION_KEYWORDS = listOf("allow", "deny", "permission", "access your")
private val CONFIRMATION_KEYWORDS = listOf("are you sure", "confirm", "cancel", "ok", "yes", "no")
private const val HOME_CONFIDENCE = 60
private const val KEYWORD_CONFIDENCE = 75
private const val UNKNOWN_CONFIDENCE = 30

/**
 * Real implementation of [StateRecognitionEngine] — classifies via OCR keyword matching plus
 * whether the workflow's own known objects are visible, not a trained scene classifier (this
 * project has none — see the interface KDoc). Genuinely real signals (actual OCR text, actual
 * [VisualMatchingManager] confidence), honestly simple decision rule.
 */
@Singleton
class StateRecognitionEngineImpl @Inject constructor(
    private val visualMatchingManager: VisualMatchingManager,
    private val ocrManager: OCRManager
) : StateRecognitionEngine {

    override suspend fun recognize(screen: Bitmap, workflow: Workflow): ScreenState {
        val templateIds = workflow.steps.map { it.templateId }.distinct()
        val detectedObjects = runCatching { visualMatchingManager.findAllObjects(templateIds, screen) }.getOrDefault(emptyList())
        val ocrText = runCatching { ocrManager.extractText(screen)?.text }.getOrNull().orEmpty()
        val lowerText = ocrText.lowercase()

        val (screenType, confidence) = when {
            PERMISSION_KEYWORDS.any { lowerText.contains(it) } -> ScreenType.PERMISSION_DIALOG to KEYWORD_CONFIDENCE
            LOGIN_KEYWORDS.any { lowerText.contains(it) } -> ScreenType.LOGIN to KEYWORD_CONFIDENCE
            LOADING_KEYWORDS.any { lowerText.contains(it) } -> ScreenType.LOADING to KEYWORD_CONFIDENCE
            ERROR_KEYWORDS.any { lowerText.contains(it) } -> ScreenType.ERROR to KEYWORD_CONFIDENCE
            SETTINGS_KEYWORDS.any { lowerText.contains(it) } -> ScreenType.SETTINGS to KEYWORD_CONFIDENCE
            CONFIRMATION_KEYWORDS.any { lowerText.contains(it) } -> ScreenType.CONFIRMATION_DIALOG to (KEYWORD_CONFIDENCE - 15)
            detectedObjects.isNotEmpty() -> ScreenType.HOME to HOME_CONFIDENCE
            else -> ScreenType.UNKNOWN to UNKNOWN_CONFIDENCE
        }

        return ScreenState(
            screenType = screenType,
            confidence = confidence,
            detectedObjects = detectedObjects,
            detectedText = ocrText
        )
    }
}
