package com.behaviorengine.core.data.teaching

import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import com.behaviorengine.core.domain.teaching.ScreenFrame
import com.behaviorengine.core.domain.teaching.SessionManager
import com.behaviorengine.core.domain.teaching.TeachingRecorder
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TouchCollectorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TeachingRecorder"

/**
 * Real implementation of [TeachingRecorder]. Runs two independent collector coroutines (frames,
 * touches) on its own scope for the session's lifetime; each frame/touch is written straight
 * through [TeachingRepository] and bumps [SessionManager]'s running counters — nothing is buffered
 * here beyond what a single `collect` call is currently processing.
 */
@Singleton
class TeachingRecorderImpl @Inject constructor(
    private val screenCaptureManager: ScreenCaptureManager,
    private val touchCollectorManager: TouchCollectorManager,
    private val teachingRepository: TeachingRepository,
    private val sessionManager: SessionManager,
    private val loggerManager: LoggerManager
) : TeachingRecorder {

    private var scope: CoroutineScope? = null
    private var frameNumber = 0

    override fun start(session: TeachingSession) {
        stop()
        frameNumber = 0
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = newScope

        newScope.launch {
            screenCaptureManager.frames.collect { captured ->
                frameNumber += 1
                val frame = ScreenFrame(
                    id = UUID.randomUUID().toString(),
                    sessionId = session.id,
                    frameNumber = frameNumber,
                    timestampMillis = captured.timestampMillis,
                    imagePath = "",
                    width = captured.width,
                    height = captured.height,
                    rotation = captured.rotation
                )
                teachingRepository.saveFrame(frame, captured.bytes)
                sessionManager.incrementFrameCount(session.id)
                loggerManager.i(TAG, "Frame saved: #$frameNumber")
            }
        }

        newScope.launch {
            touchCollectorManager.touches.collect { touch ->
                teachingRepository.saveTouch(touch)
                sessionManager.incrementTouchCount(session.id)
                loggerManager.i(TAG, "Touch recorded: (${touch.x}, ${touch.y})")
            }
        }
    }

    override fun stop() {
        scope?.cancel()
        scope = null
    }
}
