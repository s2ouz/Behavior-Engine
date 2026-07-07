package com.behaviorengine.core.data.ai

import com.behaviorengine.core.domain.ai.ContextManager
import com.behaviorengine.core.domain.ai.RuntimeContext
import com.behaviorengine.core.domain.ai.ScreenState
import com.behaviorengine.core.domain.ai.Workflow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContextManagerImpl @Inject constructor() : ContextManager {

    override fun buildContext(workflow: Workflow, currentStep: Int, screenState: ScreenState, activePackage: String): RuntimeContext =
        RuntimeContext(
            workflowId = workflow.id,
            currentStep = currentStep,
            screenType = screenState.screenType,
            activePackage = activePackage,
            detectedObjects = screenState.detectedObjects,
            ocrText = screenState.detectedText,
            variables = emptyMap(),
            timestamp = System.currentTimeMillis()
        )
}
