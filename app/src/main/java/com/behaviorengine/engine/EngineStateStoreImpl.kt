package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineClock
import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EngineHealthMonitor
import com.behaviorengine.core.domain.engine.EngineHealthSnapshot
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineSession
import com.behaviorengine.core.domain.engine.EngineState
import com.behaviorengine.core.domain.engine.EngineStateStore
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.core.domain.engine.EventBus
import com.behaviorengine.core.domain.engine.ModuleRegistry
import com.behaviorengine.core.domain.engine.PerformanceSnapshot
import com.behaviorengine.core.domain.engine.PerformanceTimer
import com.behaviorengine.di.EngineCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [EngineStateStore]. [session] is deliberately computed as
 * `combine(sessionIdentity, engineState)` rather than accumulated imperatively: [sessionIdentity]
 * only ever changes on a session boundary (a sparse [EngineEvent.LifecycleChanged] from OFFLINE,
 * or back to it), while tick/fps/status ride along for free from [engineState] — one writer per
 * field, no risk of two collectors racing to update the same [MutableStateFlow].
 *
 * [EngineSession.elapsedTimeMillis] is recomputed only when [engineState] re-emits (i.e. on a
 * status change or a tick), so — like [EngineClockSnapshot.uptimeMillis] before it — it holds
 * its last value rather than advancing in real time while PAUSED; this is a deliberate,
 * documented simplification rather than a bug, consistent with how [EngineClock] already treats
 * paused time as frozen.
 */
@Singleton
class EngineStateStoreImpl @Inject constructor(
    lifecycleManager: EngineLifecycleManager,
    clock: EngineClock,
    private val moduleRegistry: ModuleRegistry,
    engineHealthMonitor: EngineHealthMonitor,
    performanceTimer: PerformanceTimer,
    eventBus: EventBus,
    @EngineCoroutineScope private val scope: CoroutineScope
) : EngineStateStore {

    override val engineState: StateFlow<EngineState> = combine(
        lifecycleManager.status,
        clock.snapshot
    ) { status, clockSnapshot ->
        EngineState(
            status = status,
            currentTick = clockSnapshot.currentTick,
            currentFps = clockSnapshot.currentFps,
            runningTimeMillis = clockSnapshot.runningTimeMillis,
            uptimeMillis = clockSnapshot.uptimeMillis,
            loadedModules = moduleRegistry.getAllModules().map { it.id }
        )
    }.stateIn(scope, SharingStarted.Eagerly, EngineState())

    override val health: StateFlow<EngineHealthSnapshot> = engineHealthMonitor.snapshot

    override val performance: StateFlow<PerformanceSnapshot> = performanceTimer.snapshot

    private val _sessionIdentity = MutableStateFlow(SessionIdentity())

    init {
        eventBus.events.onEach { event ->
            if (event !is EngineEvent.LifecycleChanged) return@onEach
            if (event.from == EngineStatus.OFFLINE && event.to == EngineStatus.INITIALIZING) {
                _sessionIdentity.value = SessionIdentity(
                    sessionId = UUID.randomUUID().toString(),
                    startTimeMillis = System.currentTimeMillis()
                )
            } else if (event.to == EngineStatus.OFFLINE) {
                _sessionIdentity.value = SessionIdentity()
            }
        }.launchIn(scope)
    }

    override val session: StateFlow<EngineSession> = combine(
        _sessionIdentity,
        engineState
    ) { identity, state ->
        EngineSession(
            sessionId = identity.sessionId,
            startTimeMillis = identity.startTimeMillis,
            elapsedTimeMillis = if (identity.startTimeMillis > 0L) {
                System.currentTimeMillis() - identity.startTimeMillis
            } else {
                0L
            },
            status = state.status,
            currentTick = state.currentTick,
            currentFps = state.currentFps
        )
    }.stateIn(scope, SharingStarted.Eagerly, EngineSession())

    private data class SessionIdentity(val sessionId: String = "", val startTimeMillis: Long = 0L)
}
