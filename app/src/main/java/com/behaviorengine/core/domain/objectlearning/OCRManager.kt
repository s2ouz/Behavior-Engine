package com.behaviorengine.core.domain.objectlearning

import android.graphics.Bitmap

/** Extracts visible text from a cropped object image, if any exists. */
interface OCRManager {

    /** Returns `null` if no text is detected in [cropped] — per spec, OCR is skipped entirely rather than storing an empty result. */
    suspend fun extractText(cropped: Bitmap): OcrResult?
}
