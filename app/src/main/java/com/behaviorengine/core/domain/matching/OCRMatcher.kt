package com.behaviorengine.core.domain.matching

import android.graphics.Bitmap
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate

data class OcrMatchScore(val textSimilarity: Float, val languageMatch: Boolean)

/** Compares OCR text/language on a candidate crop against [ObjectTemplate.ocrText]/[ObjectTemplate.language]. */
interface OCRMatcher {
    /** `null` if [template] has no OCR text — OCR is excluded from [ConfidenceEngine]'s weighting entirely rather than scored 0, per spec ("Ignore OCR if template has no text"). */
    suspend fun score(template: ObjectTemplate, candidate: Bitmap): OcrMatchScore?
}
