package com.behaviorengine.core.data.matching

import android.graphics.Bitmap
import android.graphics.Color
import com.behaviorengine.core.domain.matching.AnalyzedScreen
import com.behaviorengine.core.domain.matching.ScreenAnalyzer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [ScreenAnalyzer]. Only the grayscale copy is contrast-stretched — the
 * color [Bitmap] itself passes through unmodified, since
 * [com.behaviorengine.core.domain.objectlearning.FeatureExtractionManager] downstream needs true
 * color pixels for dominant-color comparison, not a normalized copy.
 */
@Singleton
class ScreenAnalyzerImpl @Inject constructor() : ScreenAnalyzer {

    override fun analyze(raw: Bitmap): AnalyzedScreen {
        val width = raw.width
        val height = raw.height
        val pixels = IntArray(width * height)
        raw.getPixels(pixels, 0, width, 0, 0, width, height)

        val gray = IntArray(pixels.size) { i -> luminance(pixels[i]) }
        val stretched = contrastStretch(gray)

        return AnalyzedScreen(bitmap = raw, grayscale = stretched, width = width, height = height)
    }

    private fun luminance(pixel: Int): Int =
        (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000

    /** Histogram stretch: remaps `[min, max]` to `[0, 255]` so a low-contrast capture (dimmed screen, dark theme) doesn't flatten edge detection downstream. */
    private fun contrastStretch(gray: IntArray): IntArray {
        var min = 255
        var max = 0
        for (value in gray) {
            if (value < min) min = value
            if (value > max) max = value
        }
        val range = (max - min).coerceAtLeast(1)
        return IntArray(gray.size) { i -> ((gray[i] - min) * 255 / range).coerceIn(0, 255) }
    }
}
