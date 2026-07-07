package com.behaviorengine.core.data.profile

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.behaviorengine.core.domain.profile.UserProfile
import com.behaviorengine.core.domain.profile.UserProfileRepository
import com.behaviorengine.di.ApplicationScope
import com.behaviorengine.di.ProfileDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [UserProfileRepository], backed by a dedicated Preferences DataStore
 * (see [ProfileDataStore]). [profile]'s [SharingStarted.Eagerly] default means its very first
 * value is [UserProfile] defaults, not yet the real persisted value — fine for reactive UI
 * consumers who will recompose a moment later once the real read lands, but wrong for a one-shot
 * routing decision, which is exactly why [awaitProfile] exists as a separate, correctness-first
 * path instead of reusing [profile]'s cached value.
 */
@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    @ProfileDataStore private val dataStore: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope
) : UserProfileRepository {

    override val profile: StateFlow<UserProfile> = dataStore.data
        .map { it.toUserProfile() }
        .stateIn(scope, SharingStarted.Eagerly, UserProfile())

    override suspend fun awaitProfile(): UserProfile = dataStore.data.first().toUserProfile()

    override suspend fun saveNickname(nickname: String) {
        dataStore.edit { prefs ->
            prefs[Keys.NICKNAME] = nickname
            if (prefs[Keys.CREATED_AT_MILLIS] == null) {
                prefs[Keys.CREATED_AT_MILLIS] = System.currentTimeMillis()
            }
        }
    }

    override suspend fun completeFirstLaunch() {
        dataStore.edit { prefs -> prefs[Keys.FIRST_LAUNCH_COMPLETED] = true }
    }

    private fun Preferences.toUserProfile(): UserProfile = UserProfile(
        nickname = this[Keys.NICKNAME].orEmpty(),
        createdAtMillis = this[Keys.CREATED_AT_MILLIS] ?: 0L,
        hasCompletedFirstLaunch = this[Keys.FIRST_LAUNCH_COMPLETED] ?: false
    )

    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val CREATED_AT_MILLIS = longPreferencesKey("created_at_millis")
        val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
    }
}
