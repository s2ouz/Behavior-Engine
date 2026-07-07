package com.behaviorengine.core.data.teaching

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Process
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.SessionManager
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SessionManager"

/**
 * Real implementation of [SessionManager]. [foregroundAppOrFallback] is a best-effort guess at
 * "what app is the user teaching on": genuinely detecting the system-wide foreground app needs
 * `PACKAGE_USAGE_STATS`, a separate user-granted special permission this phase's spec doesn't
 * list — when it isn't granted, this falls back to this app's own package rather than guessing
 * wrong or crashing, per "Handle denied permissions gracefully."
 */
@Singleton
class SessionManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TeachingRepository,
    private val loggerManager: LoggerManager
) : SessionManager {

    override suspend fun createSession(): TeachingSession {
        val metrics = context.resources.displayMetrics
        val (packageName, applicationName) = foregroundAppOrFallback()
        val session = TeachingSession(
            id = UUID.randomUUID().toString(),
            name = "Session ${System.currentTimeMillis()}",
            status = TeachingState.PREPARING,
            createdAtMillis = System.currentTimeMillis(),
            packageName = packageName,
            applicationName = applicationName,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            androidVersion = Build.VERSION.RELEASE,
            screenWidth = metrics.widthPixels,
            screenHeight = metrics.heightPixels,
            density = metrics.density,
            orientation = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                "landscape"
            } else {
                "portrait"
            }
        )
        repository.saveSession(session)
        loggerManager.i(TAG, "Session created: ${session.id}")
        return session
    }

    override suspend fun startSession(sessionId: String): TeachingSession =
        transition(sessionId) { it.copy(status = TeachingState.RECORDING, startedAtMillis = it.startedAtMillis ?: System.currentTimeMillis()) }

    override suspend fun pauseSession(sessionId: String): TeachingSession =
        transition(sessionId) { it.copy(status = TeachingState.PAUSED) }

    override suspend fun resumeSession(sessionId: String): TeachingSession =
        transition(sessionId) { it.copy(status = TeachingState.RECORDING) }

    override suspend fun finishSession(sessionId: String): TeachingSession =
        transition(sessionId) { session ->
            val finishedAt = System.currentTimeMillis()
            session.copy(
                status = TeachingState.COMPLETED,
                finishedAtMillis = finishedAt,
                durationMillis = finishedAt - (session.startedAtMillis ?: finishedAt)
            )
        }

    override suspend fun cancelSession(sessionId: String): TeachingSession =
        transition(sessionId) { session ->
            val finishedAt = System.currentTimeMillis()
            session.copy(
                status = TeachingState.CANCELLED,
                finishedAtMillis = finishedAt,
                durationMillis = finishedAt - (session.startedAtMillis ?: finishedAt)
            )
        }

    override suspend fun loadSession(sessionId: String): TeachingSession? = repository.loadSession(sessionId)

    override suspend fun deleteSession(sessionId: String) = repository.deleteSession(sessionId)

    override suspend fun incrementFrameCount(sessionId: String): TeachingSession =
        transition(sessionId) { it.copy(frameCount = it.frameCount + 1) }

    override suspend fun incrementTouchCount(sessionId: String): TeachingSession =
        transition(sessionId) { it.copy(touchCount = it.touchCount + 1) }

    private suspend fun transition(sessionId: String, mutate: (TeachingSession) -> TeachingSession): TeachingSession {
        val existing = requireNotNull(repository.loadSession(sessionId)) { "No teaching session with id $sessionId" }
        val updated = mutate(existing)
        repository.updateSession(updated)
        return updated
    }

    @Suppress("DEPRECATION") // MOVE_TO_FOREGROUND/checkOpNoThrow: their API 29+ replacements would drop minSdk 24 support
    private fun foregroundAppOrFallback(): Pair<String, String> {
        val ownPackage = context.packageName
        val ownLabel = appLabel(ownPackage)
        if (!hasUsageAccess()) return ownPackage to ownLabel

        return runCatching {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val end = System.currentTimeMillis()
            val start = end - USAGE_EVENTS_LOOKBACK_MILLIS
            val events = usageStatsManager.queryEvents(start, end)
            var lastForegroundPackage: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    lastForegroundPackage = event.packageName
                }
            }
            val resolved = lastForegroundPackage ?: return@runCatching ownPackage to ownLabel
            resolved to appLabel(resolved)
        }.getOrElse {
            loggerManager.w(TAG, "Foreground app detection failed, falling back to own package: ${it.message}")
            ownPackage to ownLabel
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun appLabel(packageName: String): String = runCatching {
        val packageManager = context.packageManager
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrDefault(packageName)

    private companion object {
        const val USAGE_EVENTS_LOOKBACK_MILLIS = 10_000L
    }
}
