package com.behaviorengine.core.domain.ai

/** [AutomationExecutor]'s outcome for one [Decision]. */
data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val executedAtMillis: Long
)
