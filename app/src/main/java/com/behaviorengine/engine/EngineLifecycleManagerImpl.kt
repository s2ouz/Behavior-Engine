package com.behaviorengine.engine

import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.EngineError
import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.core.domain.engine.EventBus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [EngineLifecycleManager]. The transition table below is the single
 * source of truth for what's legal; [com.behaviorengine.engine.EngineManagerImpl] never checks
 * [EngineStatus] values itself, it just calls [transitionTo] and reacts to the boolean result.
 */
@Singleton
class EngineLifecycleManagerImpl @Inject constructor(
    private val eventBus: EventBus,
    private val loggerManager: LoggerManager
) : EngineLifecycleManager {

    private val _status = MutableStateFlow(EngineStatus.OFFLINE)
    override val status: StateFlow<EngineStatus> = _status.asStateFlow()

    override fun canTransitionTo(target: EngineStatus): Boolean =
        target in LEGAL_TRANSITIONS[_status.value].orEmpty()

    override fun transitionTo(target: EngineStatus): Boolean {
        val current = _status.value
        if (!canTransitionTo(target)) {
            loggerManager.w(TAG, "Rejected illegal transition: $current -> $target")
            return false
        }
        _status.value = target
        eventBus.publish(EngineEvent.LifecycleChanged(current, target))
        return true
    }

    override fun forceError(error: EngineError) {
        val current = _status.value
        _status.value = EngineStatus.ERROR
        loggerManager.e(TAG, "Forced ERROR from $current: ${error.message}")
        eventBus.publish(EngineEvent.LifecycleChanged(current, EngineStatus.ERROR))
        eventBus.publish(EngineEvent.Error(error))
    }

    private companion object {
        const val TAG = "EngineLifecycle"

        /**
         * Every legal next-state for each current state. Absence of an entry, or absence of a
         * target from its set, means that transition is rejected — e.g. RUNNING's set has no
         * INITIALIZING, so RUNNING -> INITIALIZING always fails.
         */
        val LEGAL_TRANSITIONS: Map<EngineStatus, Set<EngineStatus>> = mapOf(
            EngineStatus.OFFLINE to setOf(EngineStatus.INITIALIZING),
            EngineStatus.INITIALIZING to setOf(EngineStatus.READY),
            EngineStatus.READY to setOf(EngineStatus.STARTING),
            EngineStatus.STARTING to setOf(EngineStatus.RUNNING),
            EngineStatus.RUNNING to setOf(EngineStatus.PAUSING, EngineStatus.STOPPING),
            EngineStatus.PAUSING to setOf(EngineStatus.PAUSED),
            EngineStatus.PAUSED to setOf(EngineStatus.RESUMING, EngineStatus.STOPPING),
            EngineStatus.RESUMING to setOf(EngineStatus.RUNNING),
            EngineStatus.STOPPING to setOf(EngineStatus.STOPPED),
            EngineStatus.STOPPED to setOf(EngineStatus.OFFLINE),
            EngineStatus.ERROR to setOf(EngineStatus.OFFLINE)
        )
    }
}
