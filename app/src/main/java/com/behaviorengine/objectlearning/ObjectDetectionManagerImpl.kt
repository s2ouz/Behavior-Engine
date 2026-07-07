package com.behaviorengine.objectlearning

import android.graphics.Bitmap
import android.graphics.Color
import com.behaviorengine.core.common.ObjectLearningLogger
import com.behaviorengine.core.domain.objectlearning.BoundingBox
import com.behaviorengine.core.domain.objectlearning.DetectionCandidate
import com.behaviorengine.core.domain.objectlearning.DetectionMethod
import com.behaviorengine.core.domain.objectlearning.ObjectDetectionManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private const val MLKIT_BASE_CONFIDENCE = 0.6f
private const val LOCAL_WINDOW_PX = 300
private const val CONTOUR_STDDEV_MULTIPLIER = 0.5
private const val FOREGROUND_SEARCH_RADIUS_PX = 40
private const val CONTOUR_MAX_FILL_RATIO = 0.85
private const val EDGE_GRADIENT_THRESHOLD = 45
private const val FALLBACK_WIDTH_PX = 160
private const val FALLBACK_HEIGHT_PX = 100
private const val FALLBACK_CONFIDENCE = 0.30f

/**
 * Real implementation of [ObjectDetectionManager]. Tier 1 (Accessibility) never produces a
 * candidate — see [DetectionMethod]'s KDoc. Tier 2 uses real on-device ML Kit object detection.
 * Tiers 3 and 4 ("Contour Detection" and "Edge Detection" in the spec) are hand-rolled: a
 * local-window flood-fill segmentation and a four-direction edge-boundary trace, respectively —
 * genuinely different, real pixel algorithms, but not OpenCV's actual contour-finding or Canny
 * edge detector. OpenCV is a native dependency (bundled `.so` libraries, non-trivial to verify
 * across ABIs) that this phase deliberately avoids in favor of dependency-free, pure-Kotlin
 * approximations that solve the same "find a bounding box around the touch point" problem. Tier 5
 * always succeeds with a fixed-size box and a confidence deliberately below the quality gate's 70%
 * threshold, since a blind guess should never look as trustworthy as a real detection.
 */
@Singleton
class ObjectDetectionManagerImpl @Inject constructor(
    private val logger: ObjectLearningLogger
) : ObjectDetectionManager {

    private val mlKitDetector by lazy {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        ObjectDetection.getClient(options)
    }

    override suspend fun detectObject(frame: Bitmap, touchX: Float, touchY: Float): DetectionCandidate {
        val candidates = generateCandidates(frame, touchX, touchY)
        return selectBestCandidate(candidates) ?: fallbackCandidate(frame, touchX, touchY)
    }

    override suspend fun generateCandidates(frame: Bitmap, touchX: Float, touchY: Float): List<DetectionCandidate> {
        val candidates = mutableListOf<DetectionCandidate>()

        runCatching { mlKitCandidate(frame, touchX, touchY) }
            .onFailure { logger.warning("ML Kit detection failed: ${it.message}") }
            .getOrNull()
            ?.let { candidates.add(it) }

        runCatching { contourCandidate(frame, touchX, touchY) }
            .onFailure { logger.warning("Contour detection failed: ${it.message}") }
            .getOrNull()
            ?.let { candidates.add(it) }

        runCatching { edgeCandidate(frame, touchX, touchY) }
            .onFailure { logger.warning("Edge detection failed: ${it.message}") }
            .getOrNull()
            ?.let { candidates.add(it) }

        candidates.add(fallbackCandidate(frame, touchX, touchY))
        return candidates
    }

    override fun selectBestCandidate(candidates: List<DetectionCandidate>): DetectionCandidate? =
        candidates.maxByOrNull { it.confidence }

    private suspend fun mlKitCandidate(frame: Bitmap, touchX: Float, touchY: Float): DetectionCandidate? {
        val inputImage = InputImage.fromBitmap(frame, 0)
        val results = mlKitDetector.process(inputImage).awaitResult()
        val containing = results.filter { obj ->
            val box = obj.boundingBox
            touchX >= box.left && touchX <= box.right && touchY >= box.top && touchY <= box.bottom
        }
        val best = containing.maxByOrNull { obj -> obj.labels.maxOfOrNull { label -> label.confidence } ?: 0f }
            ?: return null
        val box = best.boundingBox
        val labelConfidence = best.labels.maxOfOrNull { it.confidence } ?: 0f
        val boundingBox = BoundingBox(box.left, box.top, box.right, box.bottom)
        return DetectionCandidate(
            boundingBox = boundingBox,
            confidence = max(labelConfidence, MLKIT_BASE_CONFIDENCE),
            method = DetectionMethod.ML_KIT_OBJECT_DETECTION,
            area = boundingBox.area,
            distanceToTouch = boundingBox.distanceTo(touchX, touchY)
        )
    }

    /** Local-window flood-fill segmentation seeded at the touch point — see class KDoc. */
    private fun contourCandidate(frame: Bitmap, touchX: Float, touchY: Float): DetectionCandidate? {
        val window = localWindow(frame, touchX, touchY)
        val gray = window.grayscale
        val mean = gray.average()
        val stdDev = sqrt(gray.fold(0.0) { acc, v -> acc + (v - mean) * (v - mean) } / gray.size)
        val threshold = stdDev * CONTOUR_STDDEV_MULTIPLIER

        fun isForeground(index: Int) = abs(gray[index] - mean) > threshold

        if (window.width * window.height == 0) return null

        // The exact touch pixel is very often part of a flat background (e.g. the middle of a
        // button), which fails `isForeground` on its own — flood-filling from there alone would
        // find nothing and immediately return null. Search outward in a small ring for the
        // nearest foreground pixel to actually seed the fill from instead.
        val (seedX, seedY) = findNearestForeground(window, ::isForeground) ?: return null

        val visited = BooleanArray(gray.size)
        val queue = ArrayDeque<Int>()
        val seedIndex = seedY * window.width + seedX
        queue.add(seedIndex)
        visited[seedIndex] = true
        var minX = seedX
        var maxX = seedX
        var minY = seedY
        var maxY = seedY
        var count = 0

        while (queue.isNotEmpty()) {
            val index: Int = queue.poll()!!
            val x = index % window.width
            val y = index / window.width
            if (!isForeground(index)) continue
            count++
            minX = min(minX, x); maxX = max(maxX, x)
            minY = min(minY, y); maxY = max(maxY, y)

            for ((dx, dy) in NEIGHBOR_OFFSETS) {
                val nx = x + dx
                val ny = y + dy
                if (nx !in 0 until window.width || ny !in 0 until window.height) continue
                val nIndex = ny * window.width + nx
                if (!visited[nIndex]) {
                    visited[nIndex] = true
                    queue.add(nIndex)
                }
            }
        }

        val fillRatio = count.toDouble() / gray.size
        if (count == 0 || fillRatio > CONTOUR_MAX_FILL_RATIO) return null

        val boundingBox = BoundingBox(
            left = window.offsetX + minX,
            top = window.offsetY + minY,
            right = window.offsetX + maxX + 1,
            bottom = window.offsetY + maxY + 1
        )
        // Confidence peaks for a moderately-sized blob; very small or near-window-filling blobs
        // are less likely to be a single distinct UI element.
        val confidence = (1.0 - abs(fillRatio - 0.25) / 0.25).coerceIn(0.0, 0.75).toFloat()
        return DetectionCandidate(boundingBox, confidence, DetectionMethod.CONTOUR_DETECTION, boundingBox.area, boundingBox.distanceTo(touchX, touchY))
    }

    /** Expanding-ring search outward from the window's seed pixel for the nearest pixel satisfying [isForeground]. */
    private fun findNearestForeground(window: LocalWindow, isForeground: (Int) -> Boolean): Pair<Int, Int>? {
        if (isForeground(window.seedY * window.width + window.seedX)) return window.seedX to window.seedY

        var radius = 1
        while (radius <= FOREGROUND_SEARCH_RADIUS_PX) {
            for (dy in -radius..radius) {
                for (dx in -radius..radius) {
                    if (max(abs(dx), abs(dy)) != radius) continue // only the ring's edge, not its interior (already checked at smaller radii)
                    val x = window.seedX + dx
                    val y = window.seedY + dy
                    if (x !in 0 until window.width || y !in 0 until window.height) continue
                    if (isForeground(y * window.width + x)) return x to y
                }
            }
            radius++
        }
        return null
    }

    /** Marches outward from the touch point along the Sobel gradient magnitude map until each direction hits a strong edge. */
    private fun edgeCandidate(frame: Bitmap, touchX: Float, touchY: Float): DetectionCandidate? {
        val window = localWindow(frame, touchX, touchY)
        if (window.width < 3 || window.height < 3) return null
        val gray = window.grayscale

        fun luminanceAt(x: Int, y: Int): Int = gray[(y.coerceIn(0, window.height - 1)) * window.width + x.coerceIn(0, window.width - 1)].toInt()

        fun gradientAt(x: Int, y: Int): Int {
            val gx = luminanceAt(x + 1, y) - luminanceAt(x - 1, y)
            val gy = luminanceAt(x, y + 1) - luminanceAt(x, y - 1)
            return abs(gx) + abs(gy)
        }

        var left = window.seedX
        while (left > 0 && gradientAt(left, window.seedY) < EDGE_GRADIENT_THRESHOLD) left--
        var right = window.seedX
        while (right < window.width - 1 && gradientAt(right, window.seedY) < EDGE_GRADIENT_THRESHOLD) right++
        var top = window.seedY
        while (top > 0 && gradientAt(window.seedX, top) < EDGE_GRADIENT_THRESHOLD) top--
        var bottom = window.seedY
        while (bottom < window.height - 1 && gradientAt(window.seedX, bottom) < EDGE_GRADIENT_THRESHOLD) bottom++

        if (right - left < 2 || bottom - top < 2) return null

        val boundingBox = BoundingBox(
            left = window.offsetX + left,
            top = window.offsetY + top,
            right = window.offsetX + right,
            bottom = window.offsetY + bottom
        )
        val edgeStrength = listOf(
            gradientAt(left, window.seedY),
            gradientAt(right, window.seedY),
            gradientAt(window.seedX, top),
            gradientAt(window.seedX, bottom)
        ).average()
        val confidence = (edgeStrength / 255.0).coerceIn(0.0, 0.65).toFloat()
        return DetectionCandidate(boundingBox, confidence, DetectionMethod.EDGE_DETECTION, boundingBox.area, boundingBox.distanceTo(touchX, touchY))
    }

    private fun fallbackCandidate(frame: Bitmap, touchX: Float, touchY: Float): DetectionCandidate {
        val halfWidth = FALLBACK_WIDTH_PX / 2
        val halfHeight = FALLBACK_HEIGHT_PX / 2
        val left = (touchX - halfWidth).toInt().coerceIn(0, max(0, frame.width - 2))
        val top = (touchY - halfHeight).toInt().coerceIn(0, max(0, frame.height - 2))
        val right = (touchX + halfWidth).toInt().coerceIn(left + 1, frame.width)
        val bottom = (touchY + halfHeight).toInt().coerceIn(top + 1, frame.height)
        val boundingBox = BoundingBox(left, top, right, bottom)
        return DetectionCandidate(boundingBox, FALLBACK_CONFIDENCE, DetectionMethod.LARGEST_CANDIDATE_AROUND_TOUCH, boundingBox.area, boundingBox.distanceTo(touchX, touchY))
    }

    private class LocalWindow(
        val offsetX: Int,
        val offsetY: Int,
        val width: Int,
        val height: Int,
        val seedX: Int,
        val seedY: Int,
        val grayscale: IntArray
    )

    private fun localWindow(frame: Bitmap, touchX: Float, touchY: Float): LocalWindow {
        val half = LOCAL_WINDOW_PX / 2
        val offsetX = (touchX.toInt() - half).coerceIn(0, max(0, frame.width - 1))
        val offsetY = (touchY.toInt() - half).coerceIn(0, max(0, frame.height - 1))
        val width = min(LOCAL_WINDOW_PX, frame.width - offsetX)
        val height = min(LOCAL_WINDOW_PX, frame.height - offsetY)
        val pixels = IntArray(width * height)
        frame.getPixels(pixels, 0, width, offsetX, offsetY, width, height)
        val grayscale = IntArray(pixels.size) { i ->
            val p = pixels[i]
            (Color.red(p) * 299 + Color.green(p) * 587 + Color.blue(p) * 114) / 1000
        }
        val seedX = (touchX.toInt() - offsetX).coerceIn(0, max(0, width - 1))
        val seedY = (touchY.toInt() - offsetY).coerceIn(0, max(0, height - 1))
        return LocalWindow(offsetX, offsetY, width, height, seedX, seedY, grayscale)
    }

    private companion object {
        val NEIGHBOR_OFFSETS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    }
}
