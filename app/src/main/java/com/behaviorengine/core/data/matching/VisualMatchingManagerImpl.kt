package com.behaviorengine.core.data.matching

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.behaviorengine.core.common.MatchingLogger
import com.behaviorengine.core.domain.matching.AnalyzedScreen
import com.behaviorengine.core.domain.matching.CandidateRegion
import com.behaviorengine.core.domain.matching.CandidateSearchEngine
import com.behaviorengine.core.domain.matching.ConfidenceEngine
import com.behaviorengine.core.domain.matching.ConfidenceInputs
import com.behaviorengine.core.domain.matching.ContextAnalyzer
import com.behaviorengine.core.domain.matching.FeatureMatcher
import com.behaviorengine.core.domain.matching.MatchQuality
import com.behaviorengine.core.domain.matching.MatchResult
import com.behaviorengine.core.domain.matching.MatchingCache
import com.behaviorengine.core.domain.matching.MatchingRepository
import com.behaviorengine.core.domain.matching.MatchingServiceConnection
import com.behaviorengine.core.domain.matching.MatchingStatistics
import com.behaviorengine.core.domain.matching.MultiScaleMatcher
import com.behaviorengine.core.domain.matching.OCRMatcher
import com.behaviorengine.core.domain.matching.ScaleMatchResult
import com.behaviorengine.core.domain.matching.ScreenAnalyzer
import com.behaviorengine.core.domain.matching.VisualMatchingManager
import com.behaviorengine.core.domain.objectlearning.BoundingBox
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val SEARCH_TIMEOUT_MILLIS = 300L
private const val MATCH_METHOD = "multiscale+feature+ocr+context"
private const val CAPTURE_RETRY_ATTEMPTS = 4
private const val CAPTURE_RETRY_DELAY_MILLIS = 120L

/**
 * Real implementation of [VisualMatchingManager] — runs the pipeline described on that interface's
 * KDoc and tracked as [MatchingStatistics]/[MatchResult] history via [MatchingRepository].
 *
 * [findObject]/[findAllObjects] each launch their actual work on [scope] (this manager's own,
 * independent of whichever coroutine calls them) and await it through a [CompletableDeferred], the
 * same pattern [com.behaviorengine.core.data.objectlearning.ObjectLearningManagerImpl] uses for
 * [job]-based [cancel] — so a caller's own scope going away doesn't silently orphan a search, and
 * [cancel] reliably stops whatever is in flight.
 *
 * Enforces the 300ms search budget by mutating a `var best` *outside* the [withTimeoutOrNull]
 * block from inside it: on timeout the block's coroutine is cancelled mid-loop, but every
 * assignment to `best` already made survives the cancellation, so "return the best available
 * result" (per spec) falls out naturally rather than needing separate timeout-handling logic.
 */
@Singleton
class VisualMatchingManagerImpl @Inject constructor(
    private val screenCaptureManager: ScreenCaptureManager,
    private val matchingServiceConnection: MatchingServiceConnection,
    private val screenAnalyzer: ScreenAnalyzer,
    private val candidateSearchEngine: CandidateSearchEngine,
    private val multiScaleMatcher: MultiScaleMatcher,
    private val featureMatcher: FeatureMatcher,
    private val ocrMatcher: OCRMatcher,
    private val contextAnalyzer: ContextAnalyzer,
    private val confidenceEngine: ConfidenceEngine,
    private val matchingCache: MatchingCache,
    private val matchingRepository: MatchingRepository,
    private val objectRepository: ObjectRepository,
    private val logger: MatchingLogger
) : VisualMatchingManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    override val isCaptureActive: StateFlow<Boolean> = screenCaptureManager.isCapturing

    override fun createCaptureIntent(): Intent = screenCaptureManager.createCaptureIntent()

    override fun startCapture(resultCode: Int, data: Intent) {
        matchingServiceConnection.connect(resultCode, data)
    }

    override fun stopCapture() {
        screenCaptureManager.stopProjection()
        screenCaptureManager.release()
        matchingServiceConnection.disconnect()
    }

    override suspend fun findObject(templateId: String, screen: Bitmap?): MatchResult? {
        val deferred = CompletableDeferred<MatchResult?>()
        job = scope.launch {
            _isRunning.value = true
            try {
                val template = objectRepository.getTemplates().firstOrNull { it.id == templateId }
                if (template == null) {
                    logger.warning("Unknown template: $templateId")
                    deferred.complete(null)
                    return@launch
                }
                val bitmap = screen ?: captureLiveScreen()
                if (bitmap == null) {
                    logger.warning("No live screen available for $templateId")
                    deferred.complete(null)
                    return@launch
                }
                deferred.complete(searchAnalyzed(template, screenAnalyzer.analyze(bitmap)))
            } catch (c: CancellationException) {
                deferred.complete(null)
                throw c
            } catch (t: Throwable) {
                logger.error("findObject failed", t)
                deferred.completeExceptionally(t)
            } finally {
                _isRunning.value = false
            }
        }
        return deferred.await()
    }

    override suspend fun findAllObjects(templateIds: List<String>?, screen: Bitmap?): List<MatchResult> {
        val deferred = CompletableDeferred<List<MatchResult>>()
        job = scope.launch {
            _isRunning.value = true
            try {
                val templates = objectRepository.getTemplates()
                    .let { all -> if (templateIds == null) all else all.filter { it.id in templateIds } }
                val bitmap = screen ?: captureLiveScreen()
                if (bitmap == null) {
                    deferred.complete(emptyList())
                    return@launch
                }
                val analyzedScreen = screenAnalyzer.analyze(bitmap)
                deferred.complete(templates.mapNotNull { template -> searchAnalyzed(template, analyzedScreen) })
            } catch (c: CancellationException) {
                deferred.complete(emptyList())
                throw c
            } catch (t: Throwable) {
                logger.error("findAllObjects failed", t)
                deferred.completeExceptionally(t)
            } finally {
                _isRunning.value = false
            }
        }
        return deferred.await()
    }

    override fun cancel() {
        job?.cancel()
    }

    override suspend fun clearCache() {
        matchingCache.clear()
    }

    /**
     * `ImageReader.acquireLatestImage()` (inside [ScreenCaptureManager.captureFrame]) only returns
     * an image once a new one has actually arrived since the last drain — against static,
     * non-animating content (like this very debug screen) there's often no fresh frame ready at
     * the exact instant of an on-demand call, even though [ScreenCaptureManager.isCapturing] is
     * true. A short retry (well within the 2 FPS capture cadence) is what makes single-shot
     * capture reliable in practice; a continuous consumer like Teaching Mode's own loop never
     * needs this because it's already polling every tick.
     */
    override suspend fun captureLiveScreen(): Bitmap? {
        if (!screenCaptureManager.isCapturing.value) return null
        repeat(CAPTURE_RETRY_ATTEMPTS) { attempt ->
            val frame = screenCaptureManager.captureFrame()
            if (frame != null) {
                val bitmap = runCatching { BitmapFactory.decodeByteArray(frame.bytes, 0, frame.bytes.size) }.getOrNull()
                if (bitmap != null) return bitmap
            }
            if (attempt < CAPTURE_RETRY_ATTEMPTS - 1) delay(CAPTURE_RETRY_DELAY_MILLIS)
        }
        return null
    }

    private suspend fun searchAnalyzed(template: ObjectTemplate, analyzedScreen: AnalyzedScreen): MatchResult? {
        logger.templateLoaded(template.id)
        logger.screenCaptured(analyzedScreen.width, analyzedScreen.height)
        logger.matchingStarted(template.id)
        matchingCache.resetStats()

        val startTime = System.currentTimeMillis()
        var best: MatchResult? = null
        var searchedRegions = 0

        withTimeoutOrNull(SEARCH_TIMEOUT_MILLIS) {
            val candidates = candidateSearchEngine.findCandidates(template, analyzedScreen)
            logger.candidatesGenerated(template.id, candidates.size)

            for (region in candidates) {
                // withTimeoutOrNull's cancellation is cooperative — a candidate whose
                // suspend calls (OCR in particular, a real ML Kit binder round-trip) all take
                // long enough on a slow device/emulator can push wall time well past
                // SEARCH_TIMEOUT_MILLIS before cancellation is even checked. An explicit
                // deadline check bounds how many *new* candidates start, independent of how
                // promptly cooperative cancellation lands — caught live, timeMs was measured at
                // 700ms+ against a 300ms budget before this check existed.
                if (System.currentTimeMillis() - startTime >= SEARCH_TIMEOUT_MILLIS) break
                searchedRegions++
                val scaleMatch = multiScaleMatcher.matchScale(template, region, analyzedScreen) ?: continue
                val candidateBitmap = cropAndResize(analyzedScreen.bitmap, scaleMatch, template.width, template.height) ?: continue
                try {
                    val confidence = confidenceEngine.computeConfidence(
                        ConfidenceInputs(
                            hashSimilarity = scaleMatch.hashSimilarity,
                            feature = featureMatcher.score(template, candidateBitmap),
                            ocr = ocrMatcher.score(template, candidateBitmap),
                            context = contextAnalyzer.score(template, region, analyzedScreen)
                        )
                    )
                    val quality = MatchQuality.fromConfidence(confidence) ?: continue
                    if (best == null || confidence > best!!.confidence) {
                        best = buildMatchResult(template, scaleMatch, confidence, quality)
                    }
                } finally {
                    candidateBitmap.recycle()
                }
            }
        }

        val processingTime = System.currentTimeMillis() - startTime
        logger.matchingFinished(template.id, best?.confidence ?: 0, processingTime)
        recordOutcome(template, best, processingTime, searchedRegions)
        return best
    }

    private fun buildMatchResult(template: ObjectTemplate, scaleMatch: ScaleMatchResult, confidence: Int, quality: MatchQuality): MatchResult {
        val boundingBox = BoundingBox(
            left = scaleMatch.sampledLeft,
            top = scaleMatch.sampledTop,
            right = scaleMatch.sampledLeft + scaleMatch.sampledWidth,
            bottom = scaleMatch.sampledTop + scaleMatch.sampledHeight
        )
        return MatchResult(
            id = UUID.randomUUID().toString(),
            templateId = template.id,
            confidence = confidence,
            boundingBox = boundingBox,
            centerX = boundingBox.centerX,
            centerY = boundingBox.centerY,
            width = boundingBox.width,
            height = boundingBox.height,
            rotation = 0f,
            scale = scaleMatch.scale,
            method = MATCH_METHOD,
            quality = quality,
            foundAtMillis = System.currentTimeMillis()
        )
    }

    private suspend fun recordOutcome(template: ObjectTemplate, best: MatchResult?, processingTimeMillis: Long, searchedRegions: Int) {
        matchingRepository.saveStatistics(
            MatchingStatistics(
                id = UUID.randomUUID().toString(),
                processingTimeMillis = processingTimeMillis,
                searchedRegions = searchedRegions,
                matchedTemplates = if (best != null) 1 else 0,
                cacheHits = matchingCache.stats.hits,
                cacheMisses = matchingCache.stats.misses,
                confidence = best?.confidence ?: 0,
                recordedAtMillis = System.currentTimeMillis()
            )
        )
        if (best != null) {
            matchingCache.putSuccessfulLocation(
                template.id,
                CandidateRegion(best.boundingBox.left, best.boundingBox.top, best.width, best.height, priority = 100, score = 1f)
            )
            matchingRepository.saveSuccessfulMatch(best)
        }
    }

    private fun cropAndResize(screenBitmap: Bitmap, scaleMatch: ScaleMatchResult, targetWidth: Int, targetHeight: Int): Bitmap? {
        val sample = runCatching {
            Bitmap.createBitmap(screenBitmap, scaleMatch.sampledLeft, scaleMatch.sampledTop, scaleMatch.sampledWidth, scaleMatch.sampledHeight)
        }.getOrNull() ?: return null
        val resized = Bitmap.createScaledBitmap(sample, targetWidth.coerceAtLeast(1), targetHeight.coerceAtLeast(1), true)
        if (sample !== resized && sample !== screenBitmap) sample.recycle()
        return resized
    }
}
