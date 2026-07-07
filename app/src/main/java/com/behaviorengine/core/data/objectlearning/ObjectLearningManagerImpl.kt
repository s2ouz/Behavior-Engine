package com.behaviorengine.core.data.objectlearning

import android.graphics.Bitmap
import android.os.Build
import com.behaviorengine.core.common.ObjectLearningLogger
import com.behaviorengine.core.domain.objectlearning.CropManager
import com.behaviorengine.core.domain.objectlearning.FeatureExtractionManager
import com.behaviorengine.core.domain.objectlearning.FrameSelectionManager
import com.behaviorengine.core.domain.objectlearning.LearnedObject
import com.behaviorengine.core.domain.objectlearning.LearningProgress
import com.behaviorengine.core.domain.objectlearning.OCRManager
import com.behaviorengine.core.domain.objectlearning.ObjectDetectionManager
import com.behaviorengine.core.domain.objectlearning.ObjectLearningManager
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import com.behaviorengine.core.domain.objectlearning.ObjectTemplateManager
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TouchSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val MIN_CONFIDENCE = 0.70f
private const val OBJECT_IMAGE_QUALITY = 90

/**
 * Real implementation of [ObjectLearningManager] — the pipeline in
 * [ObjectDetectionManager]'s KDoc, run once per touch: load the closest frame, detect, crop, mask,
 * extract features, OCR, build a template, save. Quality rules (confidence, crop size, frame
 * availability) each reject just the one touch, never the whole session, per "never block UI."
 *
 * Resume works by construction: [processSession] fetches
 * [ObjectRepository.getObjectsForSession] up front and skips any touch that already has a
 * [LearnedObject], so re-calling [startLearning] for a session that was stopped or killed
 * mid-run only processes what's left.
 */
@Singleton
class ObjectLearningManagerImpl @Inject constructor(
    private val teachingRepository: TeachingRepository,
    private val frameSelectionManager: FrameSelectionManager,
    private val objectDetectionManager: ObjectDetectionManager,
    private val cropManager: CropManager,
    private val featureExtractionManager: FeatureExtractionManager,
    private val ocrManager: OCRManager,
    private val objectTemplateManager: ObjectTemplateManager,
    private val objectRepository: ObjectRepository,
    private val logger: ObjectLearningLogger
) : ObjectLearningManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var cancelRequested = false
    private var stopRequested = false

    private val _isRunning = MutableStateFlow(false)
    override val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _progress = MutableStateFlow<LearningProgress?>(null)
    override val progress: StateFlow<LearningProgress?> = _progress.asStateFlow()

    override fun startLearning(sessionId: String) {
        if (_isRunning.value) return
        cancelRequested = false
        stopRequested = false
        job = scope.launch { processSession(sessionId) }
    }

    override fun stopLearning() {
        stopRequested = true
    }

    override fun cancel() {
        cancelRequested = true
        job?.cancel()
    }

    override suspend fun processSession(sessionId: String) {
        _isRunning.value = true
        val startTime = System.currentTimeMillis()
        try {
            val touches = teachingRepository.getTouches(sessionId)
            val alreadyLearnedTouchIds = objectRepository.getObjectsForSession(sessionId).map { it.touchId }.toSet()
            val remaining = touches.filterNot { it.id in alreadyLearnedTouchIds }
            var objectsLearned = alreadyLearnedTouchIds.size

            for ((index, touch) in remaining.withIndex()) {
                if (cancelRequested || stopRequested) break

                val elapsedMillis = System.currentTimeMillis() - startTime
                val perTouchEstimate = if (index > 0) elapsedMillis / index else 0L
                _progress.value = LearningProgress(
                    sessionId = sessionId,
                    currentTouchIndex = alreadyLearnedTouchIds.size + index + 1,
                    totalTouches = touches.size,
                    objectsLearned = objectsLearned,
                    currentConfidence = _progress.value?.currentConfidence ?: 0f,
                    estimatedRemainingMillis = perTouchEstimate * (remaining.size - index)
                )

                val learned = processTouch(sessionId, touch)
                if (learned != null) {
                    objectsLearned++
                    _progress.value = _progress.value?.copy(objectsLearned = objectsLearned, currentConfidence = learned.confidence)
                }
            }
            logger.learningCompleted(sessionId, objectsLearned)
        } finally {
            _isRunning.value = false
            _progress.value = null
        }
    }

    override suspend fun processTouch(sessionId: String, touch: TouchSample): LearnedObject? {
        val frame = frameSelectionManager.findClosestFrame(sessionId, touch)
        if (frame == null || !frameSelectionManager.validateFrame(frame, touch)) {
            logger.warning("No frame within tolerance for touch ${touch.id}")
            return null
        }
        logger.frameLoaded(frame.id)

        val bitmap = frameSelectionManager.loadFrame(frame) ?: run {
            logger.warning("Frame image unavailable/corrupted for touch ${touch.id}")
            return null
        }

        val candidate = objectDetectionManager.detectObject(bitmap, touch.x, touch.y)
        logger.objectDetected(candidate.method.name, candidate.confidence)
        if (candidate.confidence < MIN_CONFIDENCE) {
            logger.warning("Rejected touch ${touch.id}: confidence ${candidate.confidence} below threshold")
            return null
        }
        if (candidate.boundingBox.width < CropManager.MIN_CROP_DIMENSION_PX ||
            candidate.boundingBox.height < CropManager.MIN_CROP_DIMENSION_PX
        ) {
            logger.warning("Rejected touch ${touch.id}: crop too small")
            return null
        }

        val cropped = runCatching { cropManager.cropObject(bitmap, candidate.boundingBox) }.getOrNull()
        if (cropped == null ||
            cropped.width < CropManager.MIN_CROP_DIMENSION_PX ||
            cropped.height < CropManager.MIN_CROP_DIMENSION_PX
        ) {
            logger.warning("Rejected touch ${touch.id}: corrupted or undersized crop")
            return null
        }

        val mask = cropManager.generateMask(cropped)
        val features = featureExtractionManager.extractFeatures(cropped, mask, candidate.boundingBox)
        val ocr = runCatching { ocrManager.extractText(cropped) }.getOrNull()
        if (ocr != null) logger.ocrFinished(ocr.text.length)

        val template = objectTemplateManager.createTemplate(features, ocr)
        objectRepository.saveTemplate(template)
        logger.templateSaved(template.id)

        val learnedObject = LearnedObject(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            touchId = touch.id,
            frameId = frame.id,
            templateId = template.id,
            objectPath = "",
            maskPath = "",
            createdAtMillis = System.currentTimeMillis(),
            confidence = candidate.confidence
        )
        val saved = objectRepository.saveObject(learnedObject, compressWebp(cropped), compressPng(mask))
        logger.touchProcessed(sessionId, touch.id)
        return saved
    }

    private fun compressWebp(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        bitmap.compress(format, OBJECT_IMAGE_QUALITY, stream)
        return stream.toByteArray()
    }

    private fun compressPng(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
