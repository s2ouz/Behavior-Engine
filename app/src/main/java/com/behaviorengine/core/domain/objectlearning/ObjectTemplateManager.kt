package com.behaviorengine.core.domain.objectlearning

/** Builds the final, persistable [ObjectTemplate] from extracted features and (optional) OCR text. */
interface ObjectTemplateManager {

    /**
     * [frameWidth]/[frameHeight] are the source frame's dimensions, used only to derive
     * [ObjectTemplate.screenPositionX]/[ObjectTemplate.screenPositionY] — pass `0` for either if
     * unavailable, and the position is stored as the "unknown" sentinel instead.
     */
    fun createTemplate(features: VisualFeatures, ocr: OcrResult?, frameWidth: Int = 0, frameHeight: Int = 0): ObjectTemplate
}
