package com.behaviorengine.objectlearning

import android.graphics.Bitmap
import com.behaviorengine.core.domain.objectlearning.BoundingBox
import com.behaviorengine.core.domain.objectlearning.OCRManager
import com.behaviorengine.core.domain.objectlearning.OcrResult
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import javax.inject.Inject
import javax.inject.Singleton

private const val PRESENCE_CONFIDENCE = 1.0f
private const val UNKNOWN_LANGUAGE_TAG = "und"

/**
 * Real implementation of [OCRManager] — on-device ML Kit Text Recognition (Latin script) +
 * Language Identification, both bundled so recognition works with no network round-trip,
 * consistent with this project's fully-local design. See [OcrResult]'s KDoc for why `confidence`
 * is presence-based rather than a true per-character score.
 */
@Singleton
class OCRManagerImpl @Inject constructor() : OCRManager {

    private val textRecognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    private val languageIdentifier by lazy { LanguageIdentification.getClient() }

    override suspend fun extractText(cropped: Bitmap): OcrResult? {
        val inputImage = InputImage.fromBitmap(cropped, 0)
        val visionText = textRecognizer.process(inputImage).awaitResult()
        val text = visionText.text.trim()
        if (text.isEmpty()) return null

        val language = runCatching { languageIdentifier.identifyLanguage(text).awaitResult() }
            .getOrDefault(UNKNOWN_LANGUAGE_TAG)

        val boxes = visionText.textBlocks.mapNotNull { it.boundingBox }
        val boundingBox = if (boxes.isEmpty()) {
            BoundingBox(0, 0, cropped.width, cropped.height)
        } else {
            BoundingBox(
                left = boxes.minOf { it.left },
                top = boxes.minOf { it.top },
                right = boxes.maxOf { it.right },
                bottom = boxes.maxOf { it.bottom }
            )
        }

        return OcrResult(
            text = text,
            language = if (language == UNKNOWN_LANGUAGE_TAG) "" else language,
            confidence = PRESENCE_CONFIDENCE,
            boundingBox = boundingBox
        )
    }
}
