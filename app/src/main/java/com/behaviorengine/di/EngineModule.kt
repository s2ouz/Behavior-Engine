package com.behaviorengine.di

import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.engine.EngineManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the domain-facing [EngineManager] contract to its concrete [EngineManagerImpl].
 * Kept separate from [AppModule] so engine wiring can grow (vision module bindings,
 * recognition module bindings...) without turning the general app module into a dumping ground.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EngineModule {

    @Binds
    @Singleton
    abstract fun bindEngineManager(impl: EngineManagerImpl): EngineManager
}
