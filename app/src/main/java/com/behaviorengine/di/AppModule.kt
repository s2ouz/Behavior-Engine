package com.behaviorengine.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.behaviorengine.core.common.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME
)

private const val SETTINGS_DATASTORE_NAME = "behavior_engine_settings"

/**
 * General application-level bindings that don't belong to a specific feature module yet.
 *
 * [LoggerManager] and [ConfigManager] don't need an entry here — they use `@Inject constructor`
 * directly, which Hilt can satisfy on its own. This module exists for dependencies that require
 * a factory method, starting with the settings [DataStore]; see [AppConstants] for the version
 * label it's paired with in the UI.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}
