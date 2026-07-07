package com.behaviorengine.core.data.matching

import android.graphics.Bitmap
import com.behaviorengine.core.domain.matching.OCRMatcher
import com.behaviorengine.core.domain.matching.OcrMatchScore
import com.behaviorengine.core.domain.objectlearning.OCRManager
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** Real implementation of [OCRMatcher] — reuses [OCRManager] (ML Kit) on the candidate crop, then compares text with normalized Levenshtein similarity. */
@Singleton
class OCRMatcherImpl @Inject constructor(
    private val ocrManager: OCRManager
) : OCRMatcher {

    override suspend fun score(template: ObjectTemplate, candidate: Bitmap): OcrMatchScore? {
        if (template.ocrText.isBlank()) return null
        val ocr = runCatching { ocrManager.extractText(candidate) }.getOrNull()
            ?: return OcrMatchScore(textSimilarity = 0f, languageMatch = false)

        val similarity = textSimilarity(template.ocrText, ocr.text)
        val languageMatch = template.language.isNotBlank() && template.language == ocr.language
        return OcrMatchScore(textSimilarity = similarity, languageMatch = languageMatch)
    }

    private fun textSimilarity(a: String, b: String): Float {
        val normA = a.trim().lowercase()
        val normB = b.trim().lowercase()
        if (normA.isEmpty() || normB.isEmpty()) return 0f
        val distance = levenshtein(normA, normB)
        val maxLength = max(normA.length, normB.length)
        return (1f - distance.toFloat() / maxLength).coerceIn(0f, 1f)
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }
}
