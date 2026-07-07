package com.behaviorengine.core.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.core.domain.engine.EngineState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Exposes [EngineManager] state to [HomeScreen] and forwards button taps back to it.
 * Holds no engine logic itself — that would duplicate [EngineManager]'s responsibility — it
 * only adapts a domain [StateFlow] to the ViewModel's lifecycle.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val engineManager: EngineManager
) : ViewModel() {

    val engineState: StateFlow<EngineState> = engineManager.engineState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = EngineState()
    )

    fun onInitializeClicked() {
        engineManager.initialize()
    }

    fun onStartClicked() {
        engineManager.start()
    }

    fun onPauseClicked() {
        engineManager.pause()
    }

    fun onResumeClicked() {
        engineManager.resume()
    }

    fun onStopClicked() {
        engineManager.stop()
    }

    fun onResetClicked() {
        engineManager.reset()
    }
}
