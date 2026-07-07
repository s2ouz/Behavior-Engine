package com.behaviorengine.di

import javax.inject.Qualifier

/**
 * Qualifies the `behavior_engine_settings` [androidx.datastore.core.DataStore], reserved for
 * [com.behaviorengine.settings.SettingsManager] once it persists for real. Needed because
 * v0.6.0 adds a second `DataStore<Preferences>` binding ([ProfileDataStore]) — Hilt can no
 * longer tell the two apart by type alone.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsDataStore

/**
 * Qualifies the `user_profile` [androidx.datastore.core.DataStore] backing
 * [com.behaviorengine.core.data.profile.UserProfileRepositoryImpl] — deliberately a separate
 * file from [SettingsDataStore]'s, since user identity and app configuration are unrelated
 * concerns that happen to both be small enough to use Preferences DataStore.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProfileDataStore
