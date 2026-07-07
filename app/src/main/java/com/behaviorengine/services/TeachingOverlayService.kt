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
import com.behaviorengine.core.domain.teaching.OverlayCallbacks
import com.behaviorengine.core.domain.teaching.OverlayManager
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import com.behaviorengine.core.domain.teaching.SessionManager
import com.behaviorengine.core.domain.teaching.TeachingModeManager
import com.behaviorengine.core.domain.teaching.TeachingRecorder
import com.behaviorengine.core.domain.teaching.TouchCollectorManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground host for Teaching Mode's `MediaProjection` capture — nothing more than that, exactly
 * how [EngineService] hosts the engine. The *only* work that must happen from inside this Service
 * is [ScreenCaptureManager.startProjection]: Android requires `getMediaProjection` to be called
 * only while a `mediaProjection`-typed foreground service is already promoted (an Android 14+
 * requirement), so [onStartCommand] promotes itself first, then starts capture, then hands off to
 * the same singleton managers [com.behaviorengine.core.data.teaching.TeachingModeManagerImpl] also
 * uses for everything else (pause/resume/stop have no such constraint and never touch this class).
 */
@AndroidEntryPoint
class TeachingOverlayService : Service() {

    @Inject lateinit var loggerManager: LoggerManager
    @Inject lateinit var screenCaptureManager: ScreenCaptureManager
    @Inject lateinit var overlayManager: OverlayManager
    @Inject lateinit var touchCollectorManager: TouchCollectorManager
    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var teachingRecorder: TeachingRecorder
    @Inject lateinit var teachingModeManager: TeachingModeManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        loggerManager.i(TAG, "TeachingOverlayService destroyed")
        super.onDestroy()
    }

    private fun startCapture(intent: Intent?) {
        // Must be the very first thing this method does: once `startForegroundService()` has
        // been called, Android requires this Service to call `startForeground()` within seconds
        // or the process crashes — so this happens unconditionally, before any validation below.
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            foregroundServiceType()
        )

        val sessionId = intent?.getStringExtra(EXTRA_SESSION_ID)
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data = intent?.getParcelableExtraCompat(EXTRA_DATA)

        if (sessionId == null || data == null) {
            loggerManager.e(TAG, "Missing session id or projection data; stopping")
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

        overlayManager.show(
            OverlayCallbacks(
                onPauseClick = teachingModeManager::pauseTeaching,
                onResumeClick = teachingModeManager::resumeTeaching,
                onStopClick = teachingModeManager::stopTeaching,
                onCancelClick = teachingModeManager::cancelTeaching
            )
        )
        touchCollectorManager.startCollecting(sessionId)

        serviceScope.launch {
            val startedSession = sessionManager.startSession(sessionId)
            teachingRecorder.start(startedSession)
            loggerManager.i(TAG, "Teaching capture pipeline started for $sessionId")
        }
    }

    @Suppress("DEPRECATION")
    private fun stopForegroundCompat() {
        stopForeground(true)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(AppConstants.PROJECT_NAME)
            .setContentText(getString(R.string.teaching_overlay_notification_text))
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.teaching_overlay_channel_name),
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
        private const val TAG = "TeachingOverlayService"
        private const val CHANNEL_ID = "teaching_overlay_channel"
        private const val NOTIFICATION_ID = 2001
        private const val ACTION_STOP = "com.behaviorengine.teaching.STOP"
        private const val EXTRA_SESSION_ID = "session_id"
        private const val EXTRA_RESULT_CODE = "result_code"
        private const val EXTRA_DATA = "data"

        /** Only [com.behaviorengine.core.data.teaching.TeachingServiceConnectionImpl] should use this. */
        fun startIntent(context: Context, sessionId: String, resultCode: Int, data: Intent): Intent =
            Intent(context, TeachingOverlayService::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, TeachingOverlayService::class.java).apply { action = ACTION_STOP }
    }
}

private fun Intent.getParcelableExtraCompat(key: String): Intent? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(key, Intent::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(key)
    }
