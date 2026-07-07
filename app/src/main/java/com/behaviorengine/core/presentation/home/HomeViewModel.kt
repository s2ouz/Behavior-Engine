package com.behaviorengine.core.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.engine.EngineHealthSnapshot
import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.core.domain.engine.EngineSession
import com.behaviorengine.core.domain.engine.EngineState
import com.behaviorengine.core.domain.engine.EngineStateStore
import com.behaviorengine.core.domain.engine.PerformanceSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STATE_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Forwards [HomeScreen] taps to [EngineManager] and [EngineStateStore]'s flows to [HomeScreen].
 * Owns no engine state of its own — per this phase's spec, only [EngineStateStore] is allowed to
 * assemble that — this class only re-scopes each [EngineStateStore] flow to
 * [viewModelScope] so Compose has something lifecycle-aware to collect.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val engineManager: EngineManager,
    stateStore: EngineStateStore
) : ViewModel() {

    val engineState: StateFlow<EngineState> = stateStore.engineState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = EngineState()
    )

    val session: StateFlow<EngineSession> = stateStore.session.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = EngineSession()
    )

    val health: StateFlow<EngineHealthSnapshot> = stateStore.health.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = EngineHealthSnapshot()
    )

    val performance: StateFlow<PerformanceSnapshot> = stateStore.performance.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = PerformanceSnapshot()
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
