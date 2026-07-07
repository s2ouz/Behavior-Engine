package com.behaviorengine.core.domain.profile

import kotlinx.coroutines.flow.StateFlow

/**
 * The single source of truth for [UserProfile], backed by DataStore in
 * [com.behaviorengine.core.data.profile.UserProfileRepositoryImpl] — the first real consumer of
 * `core.data`, reserved since v0.1.0 for exactly this ("repository implementations backing
 * core.domain contracts").
 */
interface UserProfileRepository {

    /** Continuously observable profile, for screens that react to it (e.g. Home's greeting). */
    val profile: StateFlow<UserProfile>

    /**
     * Reads the current profile directly from storage, waiting for the real persisted value
     * rather than [profile]'s initial in-memory default. Used exactly once, by the splash
     * screen's routing decision — anywhere else, prefer observing [profile].
     */
    suspend fun awaitProfile(): UserProfile

    /** Persists [nickname], stamping [UserProfile.createdAtMillis] the first time this is called. */
    suspend fun saveNickname(nickname: String)

    /** Marks onboarding as finished so future launches route straight to Home. */
    suspend fun completeFirstLaunch()
}
