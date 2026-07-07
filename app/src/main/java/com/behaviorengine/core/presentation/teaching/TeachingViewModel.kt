package com.behaviorengine.core.presentation.teaching

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.behaviorengine.core.domain.objectlearning.LearningProgress
import com.behaviorengine.core.domain.objectlearning.ObjectLearningManager
import com.behaviorengine.core.domain.teaching.TeachingModeManager
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingState
import com.behaviorengine.core.domain.teaching.TeachingStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Drives [TeachingScreen]. Deliberately thin — every action is a one-line delegation to
 * [TeachingModeManager] or [ObjectLearningManager], both of which own all real orchestration;
 * this class exists only so the screen never depends on either directly.
 */
@HiltViewModel
class TeachingViewModel @Inject constructor(
    private val teachingModeManager: TeachingModeManager,
    private val teachingStorage: TeachingStorage,
    private val objectLearningManager: ObjectLearningManager
) : ViewModel() {

    val currentSession: StateFlow<TeachingSession?> = teachingModeManager.currentSession

    val currentState: StateFlow<TeachingState> = teachingModeManager.currentState

    val isLearning: StateFlow<Boolean> = objectLearningManager.isRunning

    val learningProgress: StateFlow<LearningProgress?> = objectLearningManager.progress

    fun createCaptureIntent(): Intent = teachingModeManager.createCaptureIntent()

    fun onMediaProjectionGranted(resultCode: Int, data: Intent) {
        teachingModeManager.startTeaching(resultCode, data)
    }

    fun onPauseClicked() = teachingModeManager.pauseTeaching()

    fun onResumeClicked() = teachingModeManager.resumeTeaching()

    /** Finishes the recording, then immediately starts learning objects from it — see [ObjectLearningManager]. */
    fun onFinishClicked() {
        val sessionId = currentSession.value?.id
        teachingModeManager.stopTeaching()
        if (sessionId != null) {
            objectLearningManager.startLearning(sessionId)
        }
    }

    fun onCancelClicked() = teachingModeManager.cancelTeaching()

    fun onCancelLearningClicked() = objectLearningManager.cancel()

    fun storageUsedBytes(): Long = teachingStorage.storageUsedBytes()
}
