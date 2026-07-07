package com.behaviorengine.core.domain.objectlearning

/** Builds the final, persistable [ObjectTemplate] from extracted features and (optional) OCR text. */
interface ObjectTemplateManager {

    fun createTemplate(features: VisualFeatures, ocr: OcrResult?): ObjectTemplate
}
