package com.behaviorengine.core.domain.objectlearning

/**
 * Recognized text within one cropped object image. `confidence` is presence-based (1.0 if any
 * text was recognized, 0.0 otherwise) rather than a true per-character score: ML Kit's on-device
 * Text Recognition API doesn't expose one (that's a Cloud Vision feature, and this project stays
 * fully on-device/offline by design — see [com.behaviorengine.objectlearning.OCRManagerImpl]).
 */
data class OcrResult(
    val text: String,
    val language: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)
