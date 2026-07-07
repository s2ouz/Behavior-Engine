package com.behaviorengine.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds [AppSettings] for the app to observe. The [DataStore] is injected and ready so a future
 * phase only has to add read/write logic here — no other class needs to change — but reading
 * from and writing to it is deliberately not implemented yet, per this phase's scope. Until then
 * this is an in-memory holder seeded with defaults.
 */
@Singleton
class SettingsManager @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    // TODO(future phase): persist to and read from `dataStore` instead of only holding state
    // in memory, and expose an update(settings: AppSettings) function once the Settings screen
    // needs to write changes back.
}
