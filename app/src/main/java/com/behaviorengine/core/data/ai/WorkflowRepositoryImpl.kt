package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.AIStorage
import com.behaviorengine.core.domain.ai.Workflow
import com.behaviorengine.core.domain.ai.WorkflowRepository
import com.behaviorengine.core.domain.ai.WorkflowStep
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import com.behaviorengine.core.domain.teaching.TeachingStorage
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [WorkflowRepository]; see [deriveFromSession]'s interface KDoc for the derivation rule. */
@Singleton
class WorkflowRepositoryImpl @Inject constructor(
    private val teachingStorage: TeachingStorage,
    private val objectRepository: ObjectRepository,
    private val aiStorage: AIStorage
) : WorkflowRepository {

    override suspend fun deriveFromSession(sessionId: String): Workflow? {
        val document = teachingStorage.readSessionDocument(sessionId) ?: return null
        val learnedByTouchId = objectRepository.getObjectsForSession(sessionId).associateBy { it.touchId }

        val steps = document.touches
            .sortedBy { it.timestampMillis }
            .mapNotNull { touch -> learnedByTouchId[touch.id] }
            .distinctBy { it.templateId }
            .mapIndexed { index, learnedObject ->
                WorkflowStep(
                    stepIndex = index,
                    templateId = learnedObject.templateId,
                    touchId = learnedObject.touchId,
                    createdAtMillis = learnedObject.createdAtMillis
                )
            }
        if (steps.isEmpty()) return null

        val workflow = Workflow(
            id = UUID.randomUUID().toString(),
            name = "Workflow from ${document.session.name}",
            sourceSessionId = sessionId,
            steps = steps,
            createdAtMillis = System.currentTimeMillis()
        )
        aiStorage.writeWorkflow(workflow)
        return workflow
    }

    override suspend fun getWorkflow(id: String): Workflow? = aiStorage.readWorkflow(id)

    override suspend fun listWorkflows(): List<Workflow> = aiStorage.listWorkflowIds().mapNotNull { aiStorage.readWorkflow(it) }
}
