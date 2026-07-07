package com.behaviorengine.di

import com.behaviorengine.core.data.teaching.TeachingManagerImpl
import com.behaviorengine.core.data.teaching.TeachingRepositoryImpl
import com.behaviorengine.core.domain.teaching.TeachingManager
import com.behaviorengine.core.domain.teaching.TeachingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds [TeachingRepository] and [TeachingManager] to their in-memory implementations. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TeachingDiModule {

    @Binds
    @Singleton
    abstract fun bindTeachingRepository(impl: TeachingRepositoryImpl): TeachingRepository

    @Binds
    @Singleton
    abstract fun bindTeachingManager(impl: TeachingManagerImpl): TeachingManager
}
