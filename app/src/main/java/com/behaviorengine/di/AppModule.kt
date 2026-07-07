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

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(
    name = PROFILE_DATASTORE_NAME
)

private const val SETTINGS_DATASTORE_NAME = "behavior_engine_settings"
private const val PROFILE_DATASTORE_NAME = "user_profile"

/**
 * General application-level bindings that don't belong to a specific feature module yet.
 *
 * [com.behaviorengine.core.common.LoggerManager] and [com.behaviorengine.core.common.ConfigManager]
 * don't need an entry here — they use `@Inject constructor` directly, which Hilt can satisfy on
 * its own. This module exists for dependencies that require a factory method: the two
 * `DataStore<Preferences>` instances (qualified by [SettingsDataStore] and [ProfileDataStore]
 * since Hilt can't otherwise tell two bindings of the same type apart), and the shared
 * [EngineCoroutineScope]/[ApplicationScope]-qualified [CoroutineScope]s.
 *
 * Neither scope is ever explicitly cancelled — by design, not oversight. Both are `@Singleton`
 * in [SingletonComponent], so they live exactly as long as the process does; there's no narrower
 * lifecycle to tie a cancellation to, the same reasoning Android's own guidance gives for an
 * application-wide "app scope."
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    @SettingsDataStore
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    @ProfileDataStore
    fun provideProfileDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.profileDataStore

    @Provides
    @Singleton
    @EngineCoroutineScope
    fun provideEngineCoroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
