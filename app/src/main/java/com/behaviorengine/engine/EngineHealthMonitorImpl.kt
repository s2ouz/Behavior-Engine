package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineHealthSnapshot
import com.behaviorengine.core.domain.engine.EngineHealthMonitor
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineObserver
import com.behaviorengine.core.domain.engine.EngineServiceConnection
import com.behaviorengine.core.domain.engine.EngineStatus
import com.behaviorengine.core.domain.engine.ModuleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EngineHealthMonitorImpl @Inject constructor(
    lifecycleManager: EngineLifecycleManager,
    serviceConnection: EngineServiceConnection,
    engineObserver: EngineObserver,
    private val moduleRegistry: ModuleRegistry
) : EngineHealthMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val snapshot: StateFlow<EngineHealthSnapshot> = combine(
        lifecycleManager.status,
        serviceConnection.isConnected,
        engineObserver.snapshot
    ) { status, serviceConnected, observed ->
        EngineHealthSnapshot(
            engineAlive = status != EngineStatus.OFFLINE,
            runtimeActive = status == EngineStatus.RUNNING,
            serviceConnected = serviceConnected,
            lifecycleValid = status != EngineStatus.ERROR,
            moduleCount = moduleRegistry.getAllModules().size,
            errorCount = observed.errorCount,
            warningCount = observed.warningCount
        )
    }.stateIn(scope, SharingStarted.Eagerly, EngineHealthSnapshot())
}
