package com.behaviorengine.core.data.teaching

import android.content.Intent
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.OverlayManager
import com.behaviorengine.core.domain.teaching.OverlayStats
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import com.behaviorengine.core.domain.teaching.SessionManager
import com.behaviorengine.core.domain.teaching.TeachingModeManager
import com.behaviorengine.core.domain.teaching.TeachingRecorder
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingServiceConnection
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingState
import com.behaviorengine.core.domain.teaching.TouchCollectorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TeachingModeManager"
private const val STATS_TICK_MILLIS = 500L
private const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Real implementation of [TeachingModeManager] — "coordinate all other managers," per spec.
 * [currentSession] derives reactively from [TeachingRepository.sessions] (via [SessionManager])
 * rather than being independently mutated: whichever component actually changes a session's state
 * ([TeachingOverlayService][com.behaviorengine.services.TeachingOverlayService] starting capture,
 * or this class pausing/resuming/finishing it) writes through the same repository, so
 * [currentSession] and [currentState] can never drift out of sync with what's actually true.
 *
 * Only [startTeaching] needs [com.behaviorengine.services.TeachingOverlayService] at all — it's
 * the one call (`MediaProjectionManager.getMediaProjection`) Android requires to happen while a
 * `mediaProjection`-typed foreground service is alive. Everything else
 * (pause/resume/stop/cancel) calls straight into the same singleton managers the Service also
 * uses, exactly how [com.behaviorengine.engine.EngineManagerImpl] leaves
 * [com.behaviorengine.services.EngineService] with nothing to do but hold the foreground promotion.
 */
@Singleton
class TeachingModeManagerImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val screenCaptureManager: ScreenCaptureManager,
    private val touchCollectorManager: TouchCollectorManager,
    private val overlayManager: OverlayManager,
    private val teachingRecorder: TeachingRecorder,
    private val teachingServiceConnection: TeachingServiceConnection,
    private val teachingRepository: TeachingRepository,
    private val loggerManager: LoggerManager
) : TeachingModeManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentSessionId = MutableStateFlow<String?>(null)

    override val currentSession: StateFlow<TeachingSession?> = combine(
        teachingRepository.sessions,
        _currentSessionId
    ) { sessions, id -> sessions.firstOrNull { it.id == id } }
        .stateIn(scope, SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS), null)

    override val currentState: StateFlow<TeachingState> = currentSession
        .map { it?.status ?: TeachingState.IDLE }
        .stateIn(scope, SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS), TeachingState.IDLE)

    private var statsJob: Job? = null

    override fun createCaptureIntent(): Intent = screenCaptureManager.createCaptureIntent()

    override fun startTeaching(resultCode: Int, data: Intent) {
        if (isTeaching()) return
        scope.launch {
            val session = sessionManager.createSession()
            _currentSessionId.value = session.id
            teachingServiceConnection.connect(session.id, resultCode, data)
            startStatsLoop()
            loggerManager.i(TAG, "Teaching started: ${session.id}")
        }
    }

    override fun pauseTeaching() {
        val sessionId = _currentSessionId.value ?: return
        if (currentState.value != TeachingState.RECORDING) return
        scope.launch {
            screenCaptureManager.pause()
            sessionManager.pauseSession(sessionId)
        }
    }

    override fun resumeTeaching() {
        val sessionId = _currentSessionId.value ?: return
        if (currentState.value != TeachingState.PAUSED) return
        scope.launch {
            screenCaptureManager.resume()
            sessionManager.resumeSession(sessionId)
        }
    }

    override fun stopTeaching() = finish(cancelled = false)

    override fun cancelTeaching() = finish(cancelled = true)

    override fun isTeaching(): Boolean =
        currentState.value == TeachingState.PREPARING ||
            currentState.value == TeachingState.RECORDING ||
            currentState.value == TeachingState.PAUSED

    private fun finish(cancelled: Boolean) {
        val sessionId = _currentSessionId.value ?: return
        scope.launch {
            statsJob?.cancel()
            teachingRecorder.stop()
            touchCollectorManager.stopCollecting()
            touchCollectorManager.clear()
            overlayManager.hide()
            screenCaptureManager.stopProjection()
            screenCaptureManager.release()
            teachingServiceConnection.disconnect()

            if (cancelled) sessionManager.cancelSession(sessionId) else sessionManager.finishSession(sessionId)
            loggerManager.i(TAG, "Teaching ${if (cancelled) "cancelled" else "finished"}: $sessionId")
            _currentSessionId.value = null
        }
    }

    private fun startStatsLoop() {
        statsJob?.cancel()
        var lastElapsedMillis = 0L
        statsJob = scope.launch {
            while (true) {
                val session = currentSession.value
                if (session != null) {
                    // Frozen while paused, exactly like TeachingScreen's own ticker — recording
                    // time shouldn't keep climbing for time spent not actually recording.
                    if (session.status == TeachingState.RECORDING) {
                        val startedAt = session.startedAtMillis ?: System.currentTimeMillis()
                        lastElapsedMillis = (System.currentTimeMillis() - startedAt).coerceAtLeast(0L)
                    }
                    overlayManager.update(
                        OverlayStats(
                            state = session.status,
                            recordingTimeMillis = lastElapsedMillis,
                            frameCount = session.frameCount,
                            touchCount = session.touchCount
                        )
                    )
                }
                delay(STATS_TICK_MILLIS)
            }
        }
    }
}
