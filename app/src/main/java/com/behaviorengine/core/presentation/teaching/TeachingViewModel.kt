package com.behaviorengine.core.presentation.teaching

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.behaviorengine.core.domain.teaching.TeachingModeManager
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingState
import com.behaviorengine.core.domain.teaching.TeachingStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Drives [TeachingScreen]. Deliberately thin — every action is a one-line delegation to
 * [TeachingModeManager], which owns all real orchestration; this class exists only so the screen
 * never depends on that manager (or [TeachingStorage]) directly.
 */
@HiltViewModel
class TeachingViewModel @Inject constructor(
    private val teachingModeManager: TeachingModeManager,
    private val teachingStorage: TeachingStorage
) : ViewModel() {

    val currentSession: StateFlow<TeachingSession?> = teachingModeManager.currentSession

    val currentState: StateFlow<TeachingState> = teachingModeManager.currentState

    fun createCaptureIntent(): Intent = teachingModeManager.createCaptureIntent()

    fun onMediaProjectionGranted(resultCode: Int, data: Intent) {
        teachingModeManager.startTeaching(resultCode, data)
    }

    fun onPauseClicked() = teachingModeManager.pauseTeaching()

    fun onResumeClicked() = teachingModeManager.resumeTeaching()

    fun onFinishClicked() = teachingModeManager.stopTeaching()

    fun onCancelClicked() = teachingModeManager.cancelTeaching()

    fun storageUsedBytes(): Long = teachingStorage.storageUsedBytes()
}
