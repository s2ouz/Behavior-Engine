package com.behaviorengine.core.common

import javax.inject.Inject
import javax.inject.Singleton

/** Named object-learning log events, funneled through [LoggerManager] like [TeachingLogger]. */
@Singleton
class ObjectLearningLogger @Inject constructor(
    private val loggerManager: LoggerManager
) {

    fun touchProcessed(sessionId: String, touchId: String) = loggerManager.i(TAG, "Touch processed: session=$sessionId touch=$touchId")

    fun frameLoaded(frameId: String) = loggerManager.i(TAG, "Frame loaded: $frameId")

    fun objectDetected(method: String, confidence: Float) = loggerManager.i(TAG, "Object detected: method=$method confidence=$confidence")

    fun ocrFinished(textLength: Int) = loggerManager.i(TAG, "OCR finished: textLength=$textLength")

    fun templateSaved(templateId: String) = loggerManager.i(TAG, "Template saved: $templateId")

    fun learningCompleted(sessionId: String, objectsLearned: Int) = loggerManager.i(TAG, "Learning completed: session=$sessionId objectsLearned=$objectsLearned")

    fun warning(message: String) = loggerManager.w(TAG, message)

    fun error(message: String, throwable: Throwable? = null) = loggerManager.e(TAG, message, throwable)

    private companion object {
        const val TAG = "ObjectLearning"
    }
}
