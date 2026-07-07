package com.behaviorengine.core.domain.ai

import kotlinx.serialization.Serializable

/**
 * One step of a learned [Workflow] — a target object to find, in recorded order. [touchId] traces
 * back to the originating [com.behaviorengine.core.domain.teaching.TouchSample], purely for
 * provenance/debugging.
 */
@Serializable
data class WorkflowStep(
    val stepIndex: Int,
    val templateId: String,
    val touchId: String,
    val createdAtMillis: Long
)

/**
 * A replayable sequence of learned steps — the thing [AIDecisionManager] adapts through. Not
 * authored directly (there's no workflow-editing UI); [WorkflowRepository.deriveFromSession]
 * builds one from an already-completed [com.behaviorengine.core.domain.teaching.TeachingSession]'s
 * confidently-learned touches, in the order they were recorded — see that function's KDoc for why
 * this is an honest, real definition of "workflow" rather than a placeholder.
 */
@Serializable
data class Workflow(
    val id: String,
    val name: String,
    val sourceSessionId: String,
    val steps: List<WorkflowStep>,
    val createdAtMillis: Long
)
