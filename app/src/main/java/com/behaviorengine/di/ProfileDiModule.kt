package com.behaviorengine.di

import com.behaviorengine.core.data.profile.UserProfileRepositoryImpl
import com.behaviorengine.core.domain.profile.UserProfileRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Binds [UserProfileRepository] to its DataStore-backed implementation. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileDiModule {

    @Binds
    @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository
}
