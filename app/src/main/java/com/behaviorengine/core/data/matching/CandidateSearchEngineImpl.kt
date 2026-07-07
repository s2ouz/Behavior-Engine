package com.behaviorengine.core.data.matching

import android.graphics.Bitmap
import com.behaviorengine.core.domain.matching.AnalyzedScreen
import com.behaviorengine.core.domain.matching.CandidateRegion
import com.behaviorengine.core.domain.matching.CandidateSearchEngine
import com.behaviorengine.core.domain.matching.MatchingCache
import com.behaviorengine.core.domain.objectlearning.OCRManager
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

private const val CACHE_PRIORITY = 100
private const val OCR_PRIORITY = 80
private const val GRID_PRIORITY = 50
private const val OCR_REGION_PADDING_FRACTION = 0.5f
private const val GRID_STRIDE_FRACTION = 0.5f
private const val MIN_CELL_PX = 24
private const val EDGE_SAMPLE_RESOLUTION = 12
private const val EDGE_THRESHOLD = 40
private const val EDGE_WEIGHT = 0.6f
private const val COLOR_WEIGHT = 0.4f
private const val MIN_GRID_SCORE = 0.35f
private const val MAX_GRID_CANDIDATES = 15
private const val MAX_CANDIDATES = 20

/**
 * Real implementation of [CandidateSearchEngine]. Three sources, combined and capped at
 * [MAX_CANDIDATES] so downstream stages never scan the whole screen at every scale:
 * 1. [MatchingCache] — the template's last successful location, if still fresh.
 * 2. A single OCR pass over the whole screen, only when the template has text — its bounding
 *    box (padded, since OCR finds text, not the whole tappable element around it) becomes one
 *    high-priority candidate.
 * 3. A grid scan sized to the template's own dimensions, scored by edge density (from
 *    [AnalyzedScreen.grayscale], already contrast-normalized) and dominant-color similarity
 *    (from one bulk-downscaled color thumbnail, not per-cell pixel sampling — the cheap way to
 *    get an approximate average color per cell without hundreds of thousands of `getPixel` calls).
 */
@Singleton
class CandidateSearchEngineImpl @Inject constructor(
    private val matchingCache: MatchingCache,
    private val ocrManager: OCRManager
) : CandidateSearchEngine {

    override suspend fun findCandidates(template: ObjectTemplate, screen: AnalyzedScreen): List<CandidateRegion> {
        val candidates = mutableListOf<CandidateRegion>()

        matchingCache.getCachedLocation(template.id)?.let { cached ->
            candidates.add(cached.copy(priority = CACHE_PRIORITY))
        }

        if (template.ocrText.isNotBlank()) {
            runCatching { ocrManager.extractText(screen.bitmap) }.getOrNull()?.let { ocr ->
                val box = ocr.boundingBox.padded(OCR_REGION_PADDING_FRACTION, screen.width, screen.height)
                if (box.width > 0 && box.height > 0) {
                    candidates.add(CandidateRegion(box.left, box.top, box.width, box.height, OCR_PRIORITY, 1f))
                }
            }
        }

        candidates += gridCandidates(template, screen)

        return candidates
            .distinctBy { (it.x / MIN_CELL_PX) to (it.y / MIN_CELL_PX) }
            .sortedWith(compareByDescending<CandidateRegion> { it.priority }.thenByDescending { it.score })
            .take(MAX_CANDIDATES)
    }

    private fun gridCandidates(template: ObjectTemplate, screen: AnalyzedScreen): List<CandidateRegion> {
        val cellWidth = template.width.coerceAtLeast(MIN_CELL_PX).coerceAtMost(screen.width)
        val cellHeight = template.height.coerceAtLeast(MIN_CELL_PX).coerceAtMost(screen.height)
        val strideX = (cellWidth * GRID_STRIDE_FRACTION).toInt().coerceAtLeast(1)
        val strideY = (cellHeight * GRID_STRIDE_FRACTION).toInt().coerceAtLeast(1)

        val cols = ((screen.width - cellWidth) / strideX) + 1
        val rows = ((screen.height - cellHeight) / strideY) + 1
        if (cols <= 0 || rows <= 0) return emptyList()

        val colorThumb = Bitmap.createScaledBitmap(screen.bitmap, cols, rows, true)
        val thumbPixels = IntArray(cols * rows)
        colorThumb.getPixels(thumbPixels, 0, cols, 0, 0, cols, rows)
        if (colorThumb !== screen.bitmap) colorThumb.recycle()

        val scored = mutableListOf<CandidateRegion>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = (col * strideX).coerceAtMost(screen.width - cellWidth)
                val y = (row * strideY).coerceAtMost(screen.height - cellHeight)
                val edgeScore = cellEdgeDensity(screen, x, y, cellWidth, cellHeight)
                val colorScore = closestColorSimilarity(thumbPixels[row * cols + col], template.dominantColors)
                val combined = edgeScore * EDGE_WEIGHT + colorScore * COLOR_WEIGHT
                if (combined > MIN_GRID_SCORE) {
                    scored.add(CandidateRegion(x, y, cellWidth, cellHeight, GRID_PRIORITY, combined))
                }
            }
        }
        return scored.sortedByDescending { it.score }.take(MAX_GRID_CANDIDATES)
    }

    private fun cellEdgeDensity(screen: AnalyzedScreen, x: Int, y: Int, w: Int, h: Int): Float {
        val step = (maxOf(w, h) / EDGE_SAMPLE_RESOLUTION).coerceAtLeast(1)
        val yStart = (y + 1).coerceAtMost(screen.height - 2)
        val yEnd = (y + h - 1).coerceAtMost(screen.height - 2)
        val xStart = (x + 1).coerceAtMost(screen.width - 2)
        val xEnd = (x + w - 1).coerceAtMost(screen.width - 2)
        if (yStart >= yEnd || xStart >= xEnd) return 0f

        var edgeCount = 0
        var total = 0
        var yy = yStart
        while (yy < yEnd) {
            var xx = xStart
            while (xx < xEnd) {
                val gx = screen.grayscale[yy * screen.width + xx + 1] - screen.grayscale[yy * screen.width + xx - 1]
                val gy = screen.grayscale[(yy + 1) * screen.width + xx] - screen.grayscale[(yy - 1) * screen.width + xx]
                if (abs(gx) + abs(gy) > EDGE_THRESHOLD) edgeCount++
                total++
                xx += step
            }
            yy += step
        }
        return if (total == 0) 0f else edgeCount.toFloat() / total
    }
}
