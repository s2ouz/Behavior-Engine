package com.behaviorengine.core.presentation.teaching

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.domain.objects.VisualObjectRepository
import com.behaviorengine.core.domain.teaching.TeachingManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TEACHING_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Drives [TeachingScreen]. There's no object-picker UI this phase — [selectedObject] is simply
 * the first object in [VisualObjectRepository.objects], since the taught-object library already
 * built in v0.7.0 is the only source of objects to teach. Pressing "Start Teaching" creates a
 * [com.behaviorengine.core.domain.teaching.TeachingSession] via [TeachingManager], immediately
 * moves it to `PREPARING`, and hands off navigation — matching this phase's "no capture yet, only
 * lifecycle" scope.
 */
@HiltViewModel
class TeachingViewModel @Inject constructor(
    visualObjectRepository: VisualObjectRepository,
    private val teachingManager: TeachingManager
) : ViewModel() {

    val selectedObject: StateFlow<VisualObject?> = visualObjectRepository.objects
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(TEACHING_SUBSCRIPTION_TIMEOUT_MILLIS), null)

    private val _navigateToSessionId = MutableSharedFlow<String>()
    val navigateToSessionId: SharedFlow<String> = _navigateToSessionId.asSharedFlow()

    fun onStartTeachingClicked() {
        val target = selectedObject.value ?: return
        viewModelScope.launch {
            val session = teachingManager.createSession(target.id)
            teachingManager.startSession(session.sessionId)
            _navigateToSessionId.emit(session.sessionId)
        }
    }
}
