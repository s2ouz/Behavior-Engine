package com.behaviorengine.core.domain.teaching

import android.content.Intent
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the Android `MediaProjection` lifecycle. Frames are captured continuously while recording
 * and emitted through [frames] — nothing in this phase inspects, analyzes, or recognizes them; see
 * [com.behaviorengine.vision.ScreenCaptureManagerImpl] for the real capture loop.
 *
 * Requesting the system's screen-capture consent can't happen synchronously from a plain
 * class — only an `Activity` can launch that intent and receive its result — so [createCaptureIntent]
 * hands back the `Intent` to launch, and [startProjection] takes the resulting `(resultCode, data)`
 * pair. This mirrors the only way Android actually allows `MediaProjectionManager` to be used.
 */
interface ScreenCaptureManager {

    /** Emits one [CapturedFrame] per capture tick while [isCapturing] is true and not paused. */
    val frames: SharedFlow<CapturedFrame>

    /** True from a successful [startProjection] until [stopProjection] or [release]. */
    val isCapturing: StateFlow<Boolean>

    /** The system consent `Intent` to launch via `ActivityResultContracts.StartActivityForResult`. */
    fun createCaptureIntent(): Intent

    /**
     * Starts the projection from the consent result and begins the capture loop. Must only be
     * called from within a foreground service already promoted with
     * `ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION` (an Android 14+ requirement) — see
     * [com.behaviorengine.services.TeachingOverlayService]. Returns `false` if projection setup fails.
     */
    fun startProjection(resultCode: Int, data: Intent): Boolean

    /** Captures and WEBP-encodes exactly one frame right now, or `null` if not currently capturing. */
    suspend fun captureFrame(): CapturedFrame?

    /** Skips capture ticks without tearing down the projection — cheap pause/resume. */
    fun pause()

    fun resume()

    /** Stops the capture loop and tears down the `VirtualDisplay`/`ImageReader`, keeping the projection object alive. */
    fun stopProjection()

    /** Fully releases the `MediaProjection` itself. Call after [stopProjection]. */
    fun release()
}
