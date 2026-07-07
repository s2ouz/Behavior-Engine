package com.behaviorengine.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.behaviorengine.R
import com.behaviorengine.core.common.AppConstants
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground host for the Visual Matching debug screen's own `MediaProjection` capture — nothing
 * more than that, exactly how [TeachingOverlayService] hosts Teaching Mode's. Shares the same
 * singleton [ScreenCaptureManager] Teaching Mode uses: if a Teaching session's projection is
 * already active when this starts, [ScreenCaptureManager.startProjection] is a no-op (`isCapturing`
 * is already true) and this service just holds its own foreground promotion until stopped.
 */
@AndroidEntryPoint
class VisualMatchingService : Service() {

    @Inject lateinit var loggerManager: LoggerManager
    @Inject lateinit var screenCaptureManager: ScreenCaptureManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startCapture(intent)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loggerManager.i(TAG, "VisualMatchingService destroyed")
        super.onDestroy()
    }

    private fun startCapture(intent: Intent?) {
        // Must be the very first thing this method does — see TeachingOverlayService's identical comment.
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(), foregroundServiceType())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtraCompat(EXTRA_DATA)
        if (data == null) {
            loggerManager.e(TAG, "Missing projection data; stopping")
            stopForegroundCompat()
            stopSelf()
            return
        }

        val started = screenCaptureManager.startProjection(resultCode, data)
        if (!started) {
            loggerManager.e(TAG, "Projection failed to start; stopping")
            stopForegroundCompat()
            stopSelf()
            return
        }
        loggerManager.i(TAG, "Visual matching capture started")
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        stopForeground(true)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(AppConstants.PROJECT_NAME)
            .setContentText(getString(R.string.matching_service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.matching_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        } else {
            0
        }

    companion object {
        private const val TAG = "VisualMatchingService"
        private const val CHANNEL_ID = "visual_matching_channel"
        private const val NOTIFICATION_ID = 3001
        private const val ACTION_STOP = "com.behaviorengine.matching.STOP"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "data"

        /** Only [com.behaviorengine.core.data.matching.MatchingServiceConnectionImpl] should use this. */
        fun startIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, VisualMatchingService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, VisualMatchingService::class.java).apply { action = ACTION_STOP }
    }
}

private fun Intent.getParcelableExtraCompat(key: String): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
