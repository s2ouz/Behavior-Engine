package com.behaviorengine.core.domain.teaching

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Top-level orchestrator for Teaching Mode — "Coordinate all other managers," per spec. Wraps
 * [SessionManager], [ScreenCaptureManager] (via [com.behaviorengine.services.TeachingOverlayService]
 * for start/stop, directly for pause/resume), [TouchCollectorManager], [OverlayManager], and
 * [TeachingRecorder] behind one seam so [com.behaviorengine.core.presentation.teaching.TeachingViewModel]
 * never touches any of them directly — the exact role
 * [com.behaviorengine.core.domain.engine.EngineManager] plays for the engine.
 *
 * [currentSession]/[currentState] are exposed as `StateFlow` rather than the spec's literal
 * `currentSession()`/`currentState()` functions, matching this project's established
 * StateFlow-driven UI pattern (every other manager/ViewModel in this codebase does the same) so
 * [com.behaviorengine.core.presentation.teaching.TeachingScreen] can reactively show live
 * recording time/frame/touch counts instead of polling.
 */
interface TeachingModeManager {

    val currentSession: StateFlow<TeachingSession?>

    val currentState: StateFlow<TeachingState>

    /** The system consent `Intent` to launch before [startTeaching] — delegates to [ScreenCaptureManager.createCaptureIntent]. */
    fun createCaptureIntent(): Intent

    /** Creates a session, starts the overlay + capture + recording pipeline from a granted MediaProjection consent result. */
    fun startTeaching(resultCode: Int, data: Intent)

    fun pauseTeaching()

    fun resumeTeaching()

    /** Ends recording normally: session moves to [TeachingState.COMPLETED] and every resource is released. */
    fun stopTeaching()

    /** Ends recording early: session moves to [TeachingState.CANCELLED] and every resource is released. */
    fun cancelTeaching()

    fun isTeaching(): Boolean
}
