package com.behaviorengine.core.data.matching

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.matching.DebugOverlayInfo
import com.behaviorengine.core.domain.matching.DebugOverlayManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DebugOverlayManager"
private const val LABEL_HEIGHT_PX = 40f
private const val LABEL_TEXT_SIZE_PX = 32f
private const val LABEL_TEXT_PADDING_PX = 16f

/** Real implementation of [DebugOverlayManager] — see that interface's KDoc for why this is a plain `View`/`Canvas` window, not `ComposeView`. */
@Singleton
class DebugOverlayManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggerManager: LoggerManager
) : DebugOverlayManager {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: MatchOverlayView? = null
    private var isShowing = false

    override fun show() {
        mainHandler.post {
            if (isShowing) return@post
            val view = MatchOverlayView(context)
            runCatching {
                windowManager.addView(view, buildLayoutParams())
                overlayView = view
                isShowing = true
            }.onFailure { loggerManager.e(TAG, "Failed to add debug overlay window", it) }
        }
    }

    override fun hide() {
        mainHandler.post {
            val view = overlayView ?: return@post
            runCatching { windowManager.removeView(view) }
            overlayView = null
            isShowing = false
        }
    }

    override fun update(info: DebugOverlayInfo?) {
        mainHandler.post {
            overlayView?.info = info
            overlayView?.invalidate()
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
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )
    }
}

/** Draws one [DebugOverlayInfo]'s bounding box + label directly, never intercepting touches. */
private class MatchOverlayView(context: Context) : View(context) {
    var info: DebugOverlayInfo? = null

    private val boxPaint = Paint().apply {
        color = Color.argb(255, 0, 230, 118)
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val textPaint = Paint().apply {
        color = Color.argb(255, 0, 230, 118)
        textSize = LABEL_TEXT_SIZE_PX
        isAntiAlias = true
    }
    private val textBackgroundPaint = Paint().apply {
        color = Color.argb(200, 0, 0, 0)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val current = info ?: return
        val box = current.boundingBox
        canvas.drawRect(box.left.toFloat(), box.top.toFloat(), box.right.toFloat(), box.bottom.toFloat(), boxPaint)

        val label = "${current.method}  ${current.confidence}%  ${current.processingTimeMillis}ms"
        val textWidth = textPaint.measureText(label)
        val labelTop = (box.top - LABEL_HEIGHT_PX).coerceAtLeast(0f)
        canvas.drawRect(box.left.toFloat(), labelTop, box.left + textWidth + LABEL_TEXT_PADDING_PX, labelTop + LABEL_HEIGHT_PX, textBackgroundPaint)
        canvas.drawText(label, box.left + LABEL_TEXT_PADDING_PX / 2f, labelTop + LABEL_HEIGHT_PX - 10f, textPaint)
    }
}
