package com.behaviorengine.core.domain.ai

/**
 * Owns [Workflow] read/write. There's no workflow-authoring UI this phase, so [deriveFromSession]
 * is the only way one comes into existence — see its KDoc for why that's an honest definition, not
 * a placeholder.
 */
interface WorkflowRepository {

    /**
     * Builds and saves a [Workflow] from an already-finished
     * [com.behaviorengine.core.domain.teaching.TeachingSession]: its [com.behaviorengine.core.domain.teaching.TouchSample]s,
     * in recorded order, filtered to only those that produced a
     * [com.behaviorengine.core.domain.objectlearning.LearnedObject] (v0.10.0's own quality gate
     * already rejected anything under 70% confidence — this phase trusts that gate rather than
     * re-deriving its own). `null` if the session doesn't exist or produced zero learned objects.
     */
    suspend fun deriveFromSession(sessionId: String): Workflow?

    suspend fun getWorkflow(id: String): Workflow?

    suspend fun listWorkflows(): List<Workflow>
}
