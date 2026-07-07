package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EngineObserver
import com.behaviorengine.core.domain.engine.EngineObserverSnapshot
import com.behaviorengine.core.domain.engine.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The reference [EventBus] subscriber: collects every [EngineEvent] for the lifetime of the
 * process and folds it into a running [EngineObserverSnapshot]. Runs on its own scope rather
 * than piggybacking on [EngineManagerImpl] because observation should keep working even if the
 * engine itself is OFFLINE or has hit ERROR — it's watching the engine, not part of it.
 */
@Singleton
class EngineObserverImpl @Inject constructor(
    eventBus: EventBus
) : EngineObserver {

    private val _snapshot = MutableStateFlow(EngineObserverSnapshot())
    override val snapshot: StateFlow<EngineObserverSnapshot> = _snapshot.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            eventBus.events.collect { event ->
                _snapshot.update { current -> current.reduce(event) }
            }
        }
    }

    private fun EngineObserverSnapshot.reduce(event: EngineEvent): EngineObserverSnapshot = when (event) {
        is EngineEvent.LifecycleChanged -> copy(lastLifecycleChange = event)
        is EngineEvent.ModuleEvent -> copy(moduleEventCount = moduleEventCount + 1)
        is EngineEvent.Warning -> copy(warningCount = warningCount + 1, lastWarning = event.message)
        is EngineEvent.Error -> copy(errorCount = errorCount + 1, lastError = event.error)
        is EngineEvent.Performance -> copy(lastPerformance = event)
    }
}
