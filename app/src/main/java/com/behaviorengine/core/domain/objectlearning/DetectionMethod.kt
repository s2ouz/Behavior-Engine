package com.behaviorengine.core.domain.objectlearning

/**
 * Detection priority order per spec, tried in this exact sequence by [ObjectDetectionManager]
 * until one produces a usable [DetectionCandidate].
 *
 * [ACCESSIBILITY_NODE] never actually fires this phase: object learning processes *completed*
 * teaching sessions after the fact, from a saved [com.behaviorengine.core.domain.teaching.ScreenFrame]
 * image — an `AccessibilityNodeInfo` tree describes the *live* screen, not a screenshot from
 * minutes ago, so there is no node tree to query for a touch that already happened. It stays in
 * the priority chain (returning no candidate) so a future phase doing *live* detection during
 * recording, rather than post-hoc, only has to fill in one method.
 */
enum class DetectionMethod {
    ACCESSIBILITY_NODE,
    ML_KIT_OBJECT_DETECTION,
    CONTOUR_DETECTION,
    EDGE_DETECTION,
    LARGEST_CANDIDATE_AROUND_TOUCH
}
