package com.behaviorengine.core.data.objectlearning

import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import com.behaviorengine.core.domain.objectlearning.ObjectTemplateManager
import com.behaviorengine.core.domain.objectlearning.OcrResult
import com.behaviorengine.core.domain.objectlearning.VisualFeatures
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ObjectTemplateManagerImpl @Inject constructor() : ObjectTemplateManager {

    override fun createTemplate(features: VisualFeatures, ocr: OcrResult?, frameWidth: Int, frameHeight: Int): ObjectTemplate =
        ObjectTemplate(
            id = UUID.randomUUID().toString(),
            width = features.width,
            height = features.height,
            visualHash = features.visualHash,
            cornerFeatureCount = features.cornerFeatureCount,
            dominantColors = features.dominantColors,
            brightness = features.averageBrightness,
            aspectRatio = features.aspectRatio,
            edgeDensity = features.edgeDensity,
            shapeDescriptor = features.shapeDescriptor,
            ocrText = ocr?.text ?: "",
            language = ocr?.language ?: "",
            ocrConfidence = ocr?.confidence ?: 0f,
            createdAtMillis = System.currentTimeMillis(),
            screenPositionX = if (frameWidth > 0) features.centerX / frameWidth else -1f,
            screenPositionY = if (frameHeight > 0) features.centerY / frameHeight else -1f
        )
}
