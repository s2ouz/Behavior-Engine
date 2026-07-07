package com.behaviorengine.core.presentation.teachingpreparation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.teaching.TeachingManager
import com.behaviorengine.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives [TeachingPreparationScreen]. The checklist itself is static per this phase's spec
 * ("Object Selected" and "Session Created" are always true by the time this screen can show;
 * "Screen Capture Permission" and "Capture Engine" are permanently disabled placeholders for a
 * future phase) — the only real behavior is [onFinishClicked], which moves the session to
 * `FINISHED` via [TeachingManager].
 */
@HiltViewModel
class TeachingPreparationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val teachingManager: TeachingManager
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle[Screen.TeachingPreparation.ARG_SESSION_ID])

    fun onFinishClicked() {
        viewModelScope.launch {
            teachingManager.finishSession(sessionId)
        }
    }
}
