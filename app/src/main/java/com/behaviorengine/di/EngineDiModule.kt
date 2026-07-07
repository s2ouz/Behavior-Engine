package com.behaviorengine.di

import com.behaviorengine.core.domain.engine.EngineClock
import com.behaviorengine.core.domain.engine.EngineDiagnosticsManager
import com.behaviorengine.core.domain.engine.EngineHealthMonitor
import com.behaviorengine.core.domain.engine.EngineLifecycleManager
import com.behaviorengine.core.domain.engine.EngineLoop
import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.core.domain.engine.EngineMetrics
import com.behaviorengine.core.domain.engine.EngineObserver
import com.behaviorengine.core.domain.engine.EngineServiceConnection
import com.behaviorengine.core.domain.engine.EngineStateStore
import com.behaviorengine.core.domain.engine.EngineValidator
import com.behaviorengine.core.domain.engine.EventBus
import com.behaviorengine.core.domain.engine.ModuleRegistry
import com.behaviorengine.core.domain.engine.PerformanceTimer
import com.behaviorengine.core.domain.engine.RuntimeController
import com.behaviorengine.engine.EngineClockImpl
import com.behaviorengine.engine.EngineDiagnosticsManagerImpl
import com.behaviorengine.engine.EngineHealthMonitorImpl
import com.behaviorengine.engine.EngineLifecycleManagerImpl
import com.behaviorengine.engine.EngineLoopImpl
import com.behaviorengine.engine.EngineManagerImpl
import com.behaviorengine.engine.EngineMetricsImpl
import com.behaviorengine.engine.EngineObserverImpl
import com.behaviorengine.engine.EngineServiceConnectionImpl
import com.behaviorengine.engine.EngineStateStoreImpl
import com.behaviorengine.engine.EngineValidatorImpl
import com.behaviorengine.engine.EventBusImpl
import com.behaviorengine.engine.ModuleRegistryImpl
import com.behaviorengine.engine.PerformanceTimerImpl
import com.behaviorengine.engine.RuntimeControllerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds every engine-internal domain contract to its concrete implementation. Named
 * "EngineDiModule" rather than "EngineModule" specifically to avoid colliding with
 * [com.behaviorengine.core.domain.engine.EngineModule] — the plugin contract future
 * subsystems (Vision, Rules, Learning...) implement — which is a completely different concept
 * that happens to share the word "module" in Hilt's sense versus the engine's sense.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineDiModule {

    @Binds
    @Singleton
    abstract fun bindEngineManager(impl: EngineManagerImpl): EngineManager

    @Binds
    @Singleton
    abstract fun bindRuntimeController(impl: RuntimeControllerImpl): RuntimeController

    @Binds
    @Singleton
    abstract fun bindEngineLifecycleManager(impl: EngineLifecycleManagerImpl): EngineLifecycleManager

    @Binds
    @Singleton
    abstract fun bindEngineClock(impl: EngineClockImpl): EngineClock

    @Binds
    @Singleton
    abstract fun bindEngineLoop(impl: EngineLoopImpl): EngineLoop

    @Binds
    @Singleton
    abstract fun bindModuleRegistry(impl: ModuleRegistryImpl): ModuleRegistry

    @Binds
    @Singleton
    abstract fun bindEventBus(impl: EventBusImpl): EventBus

    @Binds
    @Singleton
    abstract fun bindEngineObserver(impl: EngineObserverImpl): EngineObserver

    @Binds
    @Singleton
    abstract fun bindEngineServiceConnection(impl: EngineServiceConnectionImpl): EngineServiceConnection

    @Binds
    @Singleton
    abstract fun bindPerformanceTimer(impl: PerformanceTimerImpl): PerformanceTimer

    @Binds
    @Singleton
    abstract fun bindEngineHealthMonitor(impl: EngineHealthMonitorImpl): EngineHealthMonitor

    @Binds
    @Singleton
    abstract fun bindEngineStateStore(impl: EngineStateStoreImpl): EngineStateStore

    @Binds
    @Singleton
    abstract fun bindEngineMetrics(impl: EngineMetricsImpl): EngineMetrics

    @Binds
    @Singleton
    abstract fun bindEngineValidator(impl: EngineValidatorImpl): EngineValidator

    @Binds
    @Singleton
    abstract fun bindEngineDiagnosticsManager(impl: EngineDiagnosticsManagerImpl): EngineDiagnosticsManager
}
