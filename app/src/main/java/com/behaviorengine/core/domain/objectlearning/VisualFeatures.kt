package com.behaviorengine.core.domain.objectlearning

/**
 * Everything [FeatureExtractionManager] measures from one cropped object image. Transient — merged
 * into an [ObjectTemplate] by [ObjectTemplateManager], never itself persisted.
 *
 * `cornerFeatureCount` stands in for the spec's separate "Corner Features" and "ORB Keypoints"
 * fields: real ORB descriptor extraction needs OpenCV, a native dependency this phase deliberately
 * avoids (see [ObjectDetectionManager]'s KDoc for the same trade-off on the detection side) — a
 * lightweight FAST-style corner-response count is a real, honest measurement, just not a full
 * binary descriptor a future phase's matcher could use for viewpoint-invariant matching.
 *
 * `shapeDescriptor` stands in for a true shape descriptor (Hu moments, Fourier descriptors): it's
 * the mask's fill ratio (opaque pixels ÷ bounding-box area) — real, cheap, and useful for basic
 * "is this roughly rectangular vs. roughly circular" comparisons, not a rotation/scale-invariant
 * descriptor.
 */
data class VisualFeatures(
    val width: Int,
    val height: Int,
    val aspectRatio: Float,
    val dominantColors: List<Int>,
    val averageBrightness: Float,
    val cornerFeatureCount: Int,
    val edgeDensity: Float,
    val shapeDescriptor: Float,
    val visualHash: String,
    val centerX: Float,
    val centerY: Float
)
