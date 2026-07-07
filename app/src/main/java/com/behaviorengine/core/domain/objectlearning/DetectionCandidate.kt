package com.behaviorengine.core.domain.objectlearning

/** One candidate bounding box proposed by a single [DetectionMethod] tier — transient, never persisted. */
data class DetectionCandidate(
    val boundingBox: BoundingBox,
    val confidence: Float,
    val method: DetectionMethod,
    val area: Int,
    val distanceToTouch: Float
)
