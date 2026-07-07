package com.behaviorengine.core.domain.ai

/**
 * Builds a [RuntimeContext] from the current [ScreenState] plus [Workflow] progress. Runs *after*
 * [StateRecognitionEngine] in [AIDecisionManager]'s actual pipeline, not before, despite the
 * spec's architecture diagram listing "Context Builder" ahead of "State Recognition" — context's
 * own field list ("current screen," "visible objects," "OCR text") is exactly [StateRecognitionEngine]'s
 * output, so it has to already exist before context can be assembled. [variables] is always empty
 * this phase — there's no workflow-variable authoring UI yet, prepared for a future phase the same
 * way [com.behaviorengine.core.domain.objects.VisualObject.reserved] reserves a slot without
 * inventing UI the spec never asked for.
 */
interface ContextManager {
    fun buildContext(workflow: Workflow, currentStep: Int, screenState: ScreenState, activePackage: String): RuntimeContext
}
