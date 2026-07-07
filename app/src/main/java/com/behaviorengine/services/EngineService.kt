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
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground host for the engine's background execution — nothing more. It holds no reference
 * to [com.behaviorengine.core.domain.engine.RuntimeController], [com.behaviorengine.core.domain.engine.ModuleRegistry],
 * or any other engine subsystem: the tick loop already runs independently on its own coroutine
 * scope (see [com.behaviorengine.engine.EngineLoopImpl]), regardless of any Android component's
 * lifecycle. What that scope does *not* survive on its own is the process being killed once
 * there's no visible Activity — this Service exists purely to keep the process (and thus that
 * scope) alive by promoting it to the foreground, with a persistent notification as the
 * required, honest signal to the user that something is still running in the background.
 *
 * Controlled exclusively through [start]/[stop] by
 * [com.behaviorengine.engine.EngineServiceConnectionImpl] — see
 * [com.behaviorengine.core.domain.engine.EngineManager]'s docs for why nothing else is allowed
 * to start or stop this Service directly. `onStartCommand` always promotes to foreground and
 * returns [START_NOT_STICKY]: restarting after a process kill is a decision only
 * [com.behaviorengine.core.domain.engine.EngineManager] should make (by calling `initialize()`
 * again), not something the platform should do unprompted while the lifecycle state machine
 * doesn't know it happened.
 */
@AndroidEntryPoint
class EngineService : Service() {

    @Inject
    lateinit var loggerManager: LoggerManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        loggerManager.i(TAG, "EngineService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            foregroundServiceType()
        )
        loggerManager.i(TAG, "EngineService promoted to foreground")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        loggerManager.i(TAG, "EngineService destroyed")
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(AppConstants.PROJECT_NAME)
            .setContentText(getString(R.string.engine_service_notification_text))
            // Reuses the launcher icon for now; a proper monochrome status-bar icon is a
            // future visual-polish item, not a runtime-foundation concern.
            .setSmallIcon(R.drawable.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.engine_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    companion object {
        private const val TAG = "EngineService"
        private const val CHANNEL_ID = "engine_runtime_channel"
        private const val NOTIFICATION_ID = 1001

        /** Only [com.behaviorengine.engine.EngineServiceConnectionImpl] should use this. */
        fun intent(context: Context): Intent = Intent(context, EngineService::class.java)
    }
}
