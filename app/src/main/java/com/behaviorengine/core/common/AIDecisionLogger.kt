package com.behaviorengine.core.common

import javax.inject.Inject
import javax.inject.Singleton

/** Named AADE log events, funneled through [LoggerManager] like [MatchingLogger]/[ObjectLearningLogger]. */
@Singleton
class AIDecisionLogger @Inject constructor(
    private val loggerManager: LoggerManager
) {

    fun stateRecognized(screenType: String, confidence: Int) = loggerManager.i(TAG, "State recognized: type=$screenType confidence=$confidence")

    fun decisionGenerated(action: String, confidence: Int) = loggerManager.i(TAG, "Decision generated: action=$action confidence=$confidence")

    fun prediction(expectedScreen: String?, probability: Float) = loggerManager.i(TAG, "Prediction: expectedScreen=$expectedScreen probability=$probability")

    fun recovery(strategy: String, succeeded: Boolean) = loggerManager.i(TAG, "Recovery: strategy=$strategy succeeded=$succeeded")

    fun memoryUpdated(kind: String) = loggerManager.d(TAG, "Memory updated: $kind")

    fun executionSuccess(action: String) = loggerManager.i(TAG, "Execution success: $action")

    fun executionFailure(action: String, reason: String) = loggerManager.w(TAG, "Execution failure: action=$action reason=$reason")

    fun warning(message: String) = loggerManager.w(TAG, message)

    fun error(message: String, throwable: Throwable? = null) = loggerManager.e(TAG, message, throwable)

    private companion object {
        const val TAG = "AIDecision"
    }
}
