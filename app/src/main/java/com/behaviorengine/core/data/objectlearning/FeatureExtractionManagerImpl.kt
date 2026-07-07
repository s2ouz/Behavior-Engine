package com.behaviorengine.core.data.objectlearning

import android.graphics.Bitmap
import android.graphics.Color
import com.behaviorengine.core.domain.objectlearning.BoundingBox
import com.behaviorengine.core.domain.objectlearning.FeatureExtractionManager
import com.behaviorengine.core.domain.objectlearning.VisualFeatures
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private const val ANALYSIS_SIZE = 32
private const val HASH_WIDTH = 9
private const val HASH_HEIGHT = 8
private const val DOMINANT_COLOR_COUNT = 3
private const val COLOR_BUCKET_SIZE = 32
private const val GRADIENT_THRESHOLD = 40
private const val CORNER_GRADIENT_THRESHOLD = 60

/**
 * Real implementation of [FeatureExtractionManager] — every measurement below is genuinely
 * computed from pixels, no placeholders. See [VisualFeatures]'s KDoc for which two fields
 * (`cornerFeatureCount`, `shapeDescriptor`) are honest simplifications of what the spec calls
 * "ORB Keypoints"/a true shape descriptor, and why.
 */
@Singleton
class FeatureExtractionManagerImpl @Inject constructor() : FeatureExtractionManager {

    override fun extractFeatures(cropped: Bitmap, mask: Bitmap, boundingBox: BoundingBox): VisualFeatures {
        val analysis = Bitmap.createScaledBitmap(cropped, ANALYSIS_SIZE, ANALYSIS_SIZE, true)
        val gray = toGrayscale(analysis)

        return VisualFeatures(
            width = cropped.width,
            height = cropped.height,
            aspectRatio = cropped.width.toFloat() / cropped.height.toFloat(),
            dominantColors = dominantColors(cropped),
            averageBrightness = averageBrightness(gray),
            cornerFeatureCount = countCornerPoints(gray),
            edgeDensity = edgeDensity(gray),
            shapeDescriptor = maskFillRatio(mask),
            visualHash = differenceHash(cropped),
            centerX = boundingBox.centerX,
            centerY = boundingBox.centerY
        )
    }

    private fun toGrayscale(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val gray = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                gray[y][x] = luminance(pixel)
            }
        }
        return gray
    }

    private fun luminance(pixel: Int): Int =
        (Color.red(pixel) * 299 + Color.green(pixel) * 587 + Color.blue(pixel) * 114) / 1000

    private fun averageBrightness(gray: Array<IntArray>): Float {
        var sum = 0L
        var count = 0
        for (row in gray) {
            for (value in row) {
                sum += value
                count++
            }
        }
        return if (count == 0) 0f else sum.toFloat() / count
    }

    /** Sobel-style gradient magnitude, thresholded — the fraction of pixels that are "edges." */
    private fun edgeDensity(gray: Array<IntArray>): Float {
        val height = gray.size
        val width = gray[0].size
        var edgeCount = 0
        var total = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val (gx, gy) = sobel(gray, x, y)
                val magnitude = abs(gx) + abs(gy)
                if (magnitude > GRADIENT_THRESHOLD) edgeCount++
                total++
            }
        }
        return if (total == 0) 0f else edgeCount.toFloat() / total
    }

    /**
     * A pixel counts as "corner-like" when both the horizontal and vertical Sobel gradients are
     * individually strong — a cheap proxy for "gradient changes in more than one direction," which
     * is the intuition behind real corner detectors (Harris, FAST) without implementing one.
     */
    private fun countCornerPoints(gray: Array<IntArray>): Int {
        val height = gray.size
        val width = gray[0].size
        var count = 0
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val (gx, gy) = sobel(gray, x, y)
                if (abs(gx) > CORNER_GRADIENT_THRESHOLD && abs(gy) > CORNER_GRADIENT_THRESHOLD) count++
            }
        }
        return count
    }

    private fun sobel(gray: Array<IntArray>, x: Int, y: Int): Pair<Int, Int> {
        val gx = (gray[y - 1][x + 1] + 2 * gray[y][x + 1] + gray[y + 1][x + 1]) -
            (gray[y - 1][x - 1] + 2 * gray[y][x - 1] + gray[y + 1][x - 1])
        val gy = (gray[y + 1][x - 1] + 2 * gray[y + 1][x] + gray[y + 1][x + 1]) -
            (gray[y - 1][x - 1] + 2 * gray[y - 1][x] + gray[y - 1][x + 1])
        return gx to gy
    }

    private fun maskFillRatio(mask: Bitmap): Float {
        val width = mask.width
        val height = mask.height
        val pixels = IntArray(width * height)
        mask.getPixels(pixels, 0, width, 0, 0, width, height)
        val opaque = pixels.count { Color.alpha(it) > 0 }
        return if (pixels.isEmpty()) 0f else opaque.toFloat() / pixels.size
    }

    private fun dominantColors(bitmap: Bitmap): List<Int> {
        val scaled = Bitmap.createScaledBitmap(bitmap, ANALYSIS_SIZE, ANALYSIS_SIZE, true)
        val pixels = IntArray(ANALYSIS_SIZE * ANALYSIS_SIZE)
        scaled.getPixels(pixels, 0, ANALYSIS_SIZE, 0, 0, ANALYSIS_SIZE, ANALYSIS_SIZE)

        val buckets = mutableMapOf<Int, Int>()
        for (pixel in pixels) {
            val bucketed = Color.rgb(
                bucket(Color.red(pixel)),
                bucket(Color.green(pixel)),
                bucket(Color.blue(pixel))
            )
            buckets[bucketed] = (buckets[bucketed] ?: 0) + 1
        }
        return buckets.entries.sortedByDescending { it.value }.take(DOMINANT_COLOR_COUNT).map { it.key }
    }

    private fun bucket(channel: Int): Int = (channel / COLOR_BUCKET_SIZE) * COLOR_BUCKET_SIZE

    /** Standard dHash: resize to 9x8 grayscale, compare each pixel to its right neighbor, pack the 64 comparison bits into a hex string. */
    private fun differenceHash(bitmap: Bitmap): String {
        val small = Bitmap.createScaledBitmap(bitmap, HASH_WIDTH, HASH_HEIGHT, true)
        var hash = 0L
        var bitIndex = 0
        for (y in 0 until HASH_HEIGHT) {
            for (x in 0 until HASH_WIDTH - 1) {
                val left = luminance(small.getPixel(x, y))
                val right = luminance(small.getPixel(x + 1, y))
                if (left > right) hash = hash or (1L shl bitIndex)
                bitIndex++
            }
        }
        return hash.toULong().toString(16).padStart(16, '0')
    }
}
