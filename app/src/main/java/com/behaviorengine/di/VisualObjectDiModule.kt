package com.behaviorengine.di

import com.behaviorengine.core.data.objects.VisualObjectRepositoryImpl
import com.behaviorengine.core.domain.objects.VisualObjectRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds [VisualObjectRepository] to its in-memory implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class VisualObjectDiModule {

    @Binds
    @Singleton
    abstract fun bindVisualObjectRepository(impl: VisualObjectRepositoryImpl): VisualObjectRepository
}
