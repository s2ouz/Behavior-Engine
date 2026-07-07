package com.behaviorengine.core.data.teaching

import android.content.Context
import android.content.res.Configuration
import android.view.MotionEvent
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.TouchCollectorManager
import com.behaviorengine.core.domain.teaching.TouchSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TouchCollectorManager"
private const val TOUCH_BUFFER_CAPACITY = 32

/** Real implementation of [TouchCollectorManager] — see its KDoc for this phase's scope note. */
@Singleton
class TouchCollectorManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggerManager: LoggerManager
) : TouchCollectorManager {

    private var sessionId: String? = null
    private var collecting = false

    private val _touches = MutableSharedFlow<TouchSample>(extraBufferCapacity = TOUCH_BUFFER_CAPACITY)
    override val touches: SharedFlow<TouchSample> = _touches.asSharedFlow()

    override fun startCollecting(sessionId: String) {
        this.sessionId = sessionId
        collecting = true
        loggerManager.i(TAG, "Collecting touches for session $sessionId")
    }

    override fun stopCollecting() {
        collecting = false
    }

    override fun recordTouch(event: MotionEvent) {
        val currentSessionId = sessionId
        if (!collecting || currentSessionId == null) return

        val metrics = context.resources.displayMetrics
        val sample = TouchSample(
            id = UUID.randomUUID().toString(),
            sessionId = currentSessionId,
            x = event.rawX,
            y = event.rawY,
            timestampMillis = System.currentTimeMillis(),
            pressure = event.pressure,
            size = event.size,
            pointerCount = event.pointerCount,
            action = actionName(event.actionMasked),
            orientation = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                "landscape"
            } else {
                "portrait"
            },
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels
        )
        if (!_touches.tryEmit(sample)) {
            loggerManager.w(TAG, "Touch buffer full, dropped a sample")
        }
    }

    override fun clear() {
        sessionId = null
        collecting = false
    }

    private fun actionName(actionMasked: Int): String = when (actionMasked) {
        MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> "DOWN"
        MotionEvent.ACTION_MOVE -> "MOVE"
        MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> "UP"
        MotionEvent.ACTION_CANCEL -> "CANCEL"
        else -> "OTHER"
    }
}
