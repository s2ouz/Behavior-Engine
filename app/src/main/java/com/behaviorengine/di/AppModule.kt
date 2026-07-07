package com.behaviorengine.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_DATASTORE_NAME
)

private const val SETTINGS_DATASTORE_NAME = "behavior_engine_settings"

/**
 * General application-level bindings that don't belong to a specific feature module yet.
 *
 * [com.behaviorengine.core.common.LoggerManager] and [com.behaviorengine.core.common.ConfigManager]
 * don't need an entry here — they use `@Inject constructor` directly, which Hilt can satisfy on
 * its own. This module exists for dependencies that require a factory method: the settings
 * [DataStore], and the shared [EngineCoroutineScope]-qualified [CoroutineScope] every engine
 * singleton that needs one collects from here instead of constructing its own.
 *
 * That scope is never explicitly cancelled — by design, not oversight. It's qualified
 * `@Singleton` in [SingletonComponent], so it lives exactly as long as the process does; there's
 * no narrower lifecycle to tie a cancellation to, the same reasoning Android's own guidance gives
 * for an application-wide "app scope."
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    @EngineCoroutineScope
    fun provideEngineCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
