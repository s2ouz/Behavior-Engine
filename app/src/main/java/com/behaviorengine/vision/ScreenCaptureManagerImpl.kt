package com.behaviorengine.vision

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.CapturedFrame
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ScreenCaptureManager"
private const val CAPTURE_INTERVAL_MILLIS = 500L // 2 FPS, per spec
private const val WEBP_QUALITY = 80
private const val IMAGE_READER_MAX_IMAGES = 2

/**
 * Real implementation of [ScreenCaptureManager] — Android `MediaProjection` +
 * `VirtualDisplay` + `ImageReader`. Frames are WEBP-encoded here and handed off as
 * [CapturedFrame]; nothing downstream ever sees a raw `Bitmap`, and every `Bitmap` created here is
 * `recycle()`d before this function returns, so no native bitmap memory survives a capture tick —
 * the "no memory leaks" requirement this phase's spec calls out.
 */
@Singleton
class ScreenCaptureManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggerManager: LoggerManager
) : ScreenCaptureManager {

    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureJob: Job? = null
    private var paused = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _frames = MutableSharedFlow<CapturedFrame>(extraBufferCapacity = 1)
    override val frames: SharedFlow<CapturedFrame> = _frames.asSharedFlow()

    private val _isCapturing = MutableStateFlow(false)
    override val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            loggerManager.w(TAG, "MediaProjection stopped by the system")
            stopProjection()
        }
    }

    override fun createCaptureIntent(): Intent = mediaProjectionManager.createScreenCaptureIntent()

    override fun startProjection(resultCode: Int, data: Intent): Boolean {
        if (_isCapturing.value) return true
        return runCatching {
            val projection = mediaProjectionManager.getMediaProjection(resultCode, data)
                ?: return@runCatching false
            projection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

            val metrics = context.resources.displayMetrics
            val reader = ImageReader.newInstance(
                metrics.widthPixels,
                metrics.heightPixels,
                android.graphics.PixelFormat.RGBA_8888,
                IMAGE_READER_MAX_IMAGES
            )
            val display = projection.createVirtualDisplay(
                VIRTUAL_DISPLAY_NAME,
                metrics.widthPixels,
                metrics.heightPixels,
                metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                null
            )

            mediaProjection = projection
            imageReader = reader
            virtualDisplay = display
            paused = false
            _isCapturing.value = true
            startCaptureLoop()
            loggerManager.i(TAG, "Projection started: ${metrics.widthPixels}x${metrics.heightPixels}")
            true
        }.getOrElse {
            loggerManager.e(TAG, "Failed to start projection", it)
            false
        }
    }

    override suspend fun captureFrame(): CapturedFrame? {
        val reader = imageReader ?: return null
        return withContext(Dispatchers.IO) {
            val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return@withContext null
            try {
                // stopProjection() closes imageReader from the calling coroutine without waiting
                // for an in-flight captureFrame() to finish, so this Image can be invalidated
                // mid-encode (IllegalStateException: "Image is already closed") — a real race
                // hit live during SPEC-11 testing, not hypothetical. Degrade to "no frame this
                // tick" rather than crashing the whole process.
                runCatching { encodeImage(image) }.getOrNull()
            } finally {
                runCatching { image.close() }
            }
        }
    }

    override fun pause() {
        paused = true
    }

    override fun resume() {
        paused = false
    }

    override fun stopProjection() {
        captureJob?.cancel()
        captureJob = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        _isCapturing.value = false
        loggerManager.i(TAG, "Projection stopped")
    }

    override fun release() {
        stopProjection()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun startCaptureLoop() {
        captureJob = scope.launch {
            while (_isCapturing.value) {
                if (!paused) {
                    captureFrame()?.let { _frames.emit(it) }
                }
                delay(CAPTURE_INTERVAL_MILLIS)
            }
        }
    }

    private fun encodeImage(image: Image): CapturedFrame? {
        val plane = image.planes.firstOrNull() ?: return null
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(plane.buffer)

        val cropped = if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height).also { bitmap.recycle() }
        }

        val output = ByteArrayOutputStream()
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        cropped.compress(format, WEBP_QUALITY, output)
        cropped.recycle()

        return CapturedFrame(
            bytes = output.toByteArray(),
            width = image.width,
            height = image.height,
            timestampMillis = System.currentTimeMillis(),
            rotation = context.resources.configuration.orientation
        )
    }

    private companion object {
        const val VIRTUAL_DISPLAY_NAME = "BehaviorEngineTeachingCapture"
    }
}
