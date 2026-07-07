package com.behaviorengine.core.data.teaching

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.OverlayCallbacks
import com.behaviorengine.core.domain.teaching.OverlayManager
import com.behaviorengine.core.domain.teaching.OverlayStats
import com.behaviorengine.core.domain.teaching.TeachingState
import com.behaviorengine.core.domain.teaching.TouchCollectorManager
import com.behaviorengine.utils.TimeFormatter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "OverlayManager"
private const val DRAG_CLICK_SLOP_PX = 12

/**
 * Real implementation of [OverlayManager] — a small draggable [LinearLayout] added directly via
 * [WindowManager], never a `ComposeView` (see the interface KDoc for why). The title row doubles
 * as the drag handle so the button row underneath keeps working as normal clickable views; every
 * touch either one sees is also forwarded to [touchCollectorManager], since these are the only
 * touches this phase can honestly observe (see [com.behaviorengine.core.domain.teaching.TouchSample]).
 */
@Singleton
class OverlayManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val touchCollectorManager: TouchCollectorManager,
    private val loggerManager: LoggerManager
) : OverlayManager {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Every method below touches Android Views, which may only ever be touched from the thread
    // that created them. TeachingModeManagerImpl's stats loop runs on Dispatchers.Default, so
    // without this hop, update() crashes with CalledFromWrongThreadException the first time it
    // fires — this makes every method here safe to call from any thread, not just the main one.
    private val mainHandler = Handler(Looper.getMainLooper())

    private var rootView: LinearLayout? = null
    private var recLabel: TextView? = null
    private var statsLabel: TextView? = null
    private var pauseResumeButton: Button? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isPaused = false

    private val _isShowing = MutableStateFlow(false)
    override val isShowing: StateFlow<Boolean> = _isShowing.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    override val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    override fun show(callbacks: OverlayCallbacks) {
        mainHandler.post {
            if (_isShowing.value) return@post

            val view = buildOverlayView(callbacks)
            val params = buildLayoutParams()
            runCatching {
                windowManager.addView(view, params)
                rootView = view
                layoutParams = params
                _isShowing.value = true
            }.onFailure { loggerManager.e(TAG, "Failed to add overlay window", it) }
        }
    }

    override fun hide() {
        mainHandler.post {
            val view = rootView ?: return@post
            runCatching { windowManager.removeView(view) }
            rootView = null
            layoutParams = null
            recLabel = null
            statsLabel = null
            pauseResumeButton = null
            isPaused = false
            _isShowing.value = false
        }
    }

    override fun update(stats: OverlayStats) {
        mainHandler.post {
            isPaused = stats.state == TeachingState.PAUSED
            recLabel?.text = if (isPaused) {
                "● PAUSED  ${TimeFormatter.formatElapsed(stats.recordingTimeMillis)}"
            } else {
                "● REC  ${TimeFormatter.formatElapsed(stats.recordingTimeMillis)}"
            }
            statsLabel?.text = "F:${stats.frameCount}  T:${stats.touchCount}"
            pauseResumeButton?.text = if (isPaused) "Resume" else "Pause"
        }
    }

    override fun move(x: Int, y: Int) {
        mainHandler.post {
            val params = layoutParams ?: return@post
            val view = rootView ?: return@post
            params.x = x
            params.y = y
            runCatching { windowManager.updateViewLayout(view, params) }
        }
    }

    override fun lock() {
        _isLocked.value = true
    }

    override fun unlock() {
        _isLocked.value = false
    }

    private fun buildOverlayView(callbacks: OverlayCallbacks): LinearLayout {
        val padding = (12 * context.resources.displayMetrics.density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            background = GradientDrawable().apply {
                cornerRadius = 8 * context.resources.displayMetrics.density
                setColor(Color.argb(230, 26, 26, 32))
            }
        }

        val titleRow = TextView(context).apply {
            text = "● REC  00:00"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        titleRow.setOnTouchListener(dragHandleTouchListener())
        recLabel = titleRow

        val stats = TextView(context).apply {
            text = "F:0  T:0"
            setTextColor(Color.LTGRAY)
            textSize = 12f
        }
        statsLabel = stats

        val buttonRow = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }

        val pauseResume = compactButton("Pause") {
            if (isPaused) callbacks.onResumeClick() else callbacks.onPauseClick()
        }
        pauseResumeButton = pauseResume

        val stop = compactButton("Stop", callbacks.onStopClick)
        val cancel = compactButton("Cancel", callbacks.onCancelClick)

        buttonRow.addView(pauseResume)
        buttonRow.addView(stop)
        buttonRow.addView(cancel)

        container.addView(titleRow)
        container.addView(stats)
        container.addView(buttonRow)
        return container
    }

    private fun compactButton(label: String, onClick: () -> Unit): Button =
        Button(context).apply {
            text = label
            textSize = 10f
            setPadding(8, 0, 8, 0)
            minWidth = 0
            minimumWidth = 0
            setOnTouchListener { _, event -> touchCollectorManager.recordTouch(event); false }
            setOnClickListener { onClick() }
        }

    private fun dragHandleTouchListener(): View.OnTouchListener {
        var startTouchX = 0f
        var startTouchY = 0f
        var startParamX = 0
        var startParamY = 0

        return View.OnTouchListener { _, event ->
            touchCollectorManager.recordTouch(event)
            if (_isLocked.value) return@OnTouchListener false

            val params = layoutParams ?: return@OnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    startParamX = params.x
                    startParamY = params.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - startTouchX).toInt()
                    val dy = (event.rawY - startTouchY).toInt()
                    if (abs(dx) > DRAG_CLICK_SLOP_PX || abs(dy) > DRAG_CLICK_SLOP_PX) {
                        move(startParamX + dx, startParamY + dy)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = OVERLAY_INITIAL_X
            y = OVERLAY_INITIAL_Y
        }
    }

    private companion object {
        const val OVERLAY_INITIAL_X = 24
        const val OVERLAY_INITIAL_Y = 200
    }
}
