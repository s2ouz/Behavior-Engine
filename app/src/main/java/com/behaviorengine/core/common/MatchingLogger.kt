package com.behaviorengine.core.common

import javax.inject.Inject
import javax.inject.Singleton

/** Named IVME log events, funneled through [LoggerManager] like [ObjectLearningLogger]/[TeachingLogger]. */
@Singleton
class MatchingLogger @Inject constructor(
    private val loggerManager: LoggerManager
) {

    fun templateLoaded(templateId: String) = loggerManager.i(TAG, "Template loaded: $templateId")

    fun screenCaptured(width: Int, height: Int) = loggerManager.i(TAG, "Screen captured: ${width}x$height")

    fun candidatesGenerated(templateId: String, count: Int) = loggerManager.i(TAG, "Candidates generated: template=$templateId count=$count")

    fun matchingStarted(templateId: String) = loggerManager.i(TAG, "Matching started: $templateId")

    fun matchingFinished(templateId: String, confidence: Int, processingTimeMillis: Long) =
        loggerManager.i(TAG, "Matching finished: template=$templateId confidence=$confidence timeMs=$processingTimeMillis")

    fun confidence(templateId: String, value: Int) = loggerManager.i(TAG, "Confidence: template=$templateId value=$value")

    fun cacheHit(templateId: String) = loggerManager.d(TAG, "Cache hit: $templateId")

    fun cacheMiss(templateId: String) = loggerManager.d(TAG, "Cache miss: $templateId")

    fun warning(message: String) = loggerManager.w(TAG, message)

    fun error(message: String, throwable: Throwable? = null) = loggerManager.e(TAG, message, throwable)

    private companion object {
        const val TAG = "VisualMatching"
    }
}
