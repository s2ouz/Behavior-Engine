package com.behaviorengine.di

import com.behaviorengine.core.data.teaching.OverlayManagerImpl
import com.behaviorengine.core.data.teaching.SessionManagerImpl
import com.behaviorengine.core.data.teaching.TeachingModeManagerImpl
import com.behaviorengine.core.data.teaching.TeachingRecorderImpl
import com.behaviorengine.core.data.teaching.TeachingRepositoryImpl
import com.behaviorengine.core.data.teaching.TeachingServiceConnectionImpl
import com.behaviorengine.core.data.teaching.TeachingStorageImpl
import com.behaviorengine.core.data.teaching.TouchCollectorManagerImpl
import com.behaviorengine.core.domain.teaching.OverlayManager
import com.behaviorengine.core.domain.teaching.ScreenCaptureManager
import com.behaviorengine.core.domain.teaching.SessionManager
import com.behaviorengine.core.domain.teaching.TeachingModeManager
import com.behaviorengine.core.domain.teaching.TeachingRecorder
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TeachingServiceConnection
import com.behaviorengine.core.domain.teaching.TeachingStorage
import com.behaviorengine.core.domain.teaching.TouchCollectorManager
import com.behaviorengine.vision.ScreenCaptureManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds every Teaching Mode contract to its real implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class TeachingDiModule {

    @Binds
    @Singleton
    abstract fun bindTeachingStorage(impl: TeachingStorageImpl): TeachingStorage

    @Binds
    @Singleton
    abstract fun bindTeachingRepository(impl: TeachingRepositoryImpl): TeachingRepository

    @Binds
    @Singleton
    abstract fun bindSessionManager(impl: SessionManagerImpl): SessionManager

    @Binds
    @Singleton
    abstract fun bindScreenCaptureManager(impl: ScreenCaptureManagerImpl): ScreenCaptureManager

    @Binds
    @Singleton
    abstract fun bindTouchCollectorManager(impl: TouchCollectorManagerImpl): TouchCollectorManager

    @Binds
    @Singleton
    abstract fun bindOverlayManager(impl: OverlayManagerImpl): OverlayManager

    @Binds
    @Singleton
    abstract fun bindTeachingRecorder(impl: TeachingRecorderImpl): TeachingRecorder

    @Binds
    @Singleton
    abstract fun bindTeachingServiceConnection(impl: TeachingServiceConnectionImpl): TeachingServiceConnection

    @Binds
    @Singleton
    abstract fun bindTeachingModeManager(impl: TeachingModeManagerImpl): TeachingModeManager
}
