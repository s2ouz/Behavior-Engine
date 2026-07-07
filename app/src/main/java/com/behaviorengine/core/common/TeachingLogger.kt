package com.behaviorengine.core.common

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Named, teaching-specific log events, funneled through [LoggerManager] like everything else in
 * the app — never [timber.log.Timber] directly. Exists so every teaching-module class logs the
 * same handful of events the same way, instead of each hand-rolling its own tag/message format.
 */
@Singleton
class TeachingLogger @Inject constructor(
    private val loggerManager: LoggerManager
) {

    fun sessionStarted(sessionId: String) = loggerManager.i(TAG, "Session started: $sessionId")

    fun frameSaved(sessionId: String, frameNumber: Int) = loggerManager.i(TAG, "Frame saved: session=$sessionId frame=$frameNumber")

    fun touchRecorded(sessionId: String, x: Float, y: Float) = loggerManager.i(TAG, "Touch recorded: session=$sessionId ($x, $y)")

    fun projectionStarted() = loggerManager.i(TAG, "Projection started")

    fun projectionStopped() = loggerManager.i(TAG, "Projection stopped")

    fun error(message: String, throwable: Throwable? = null) = loggerManager.e(TAG, message, throwable)

    fun warning(message: String) = loggerManager.w(TAG, message)

    private companion object {
        const val TAG = "Teaching"
    }
}
