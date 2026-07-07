package com.behaviorengine.core.domain.profile

/**
 * The local, offline-only identity of the person using this device — nothing more. There is no
 * login, no email, no account: [nickname] is the entire "who are you" the product ever asks for,
 * per this phase's product vision of staying fully local.
 *
 * @param nickname Chosen at onboarding (see [UserProfileRepository.saveNickname]); empty before
 * onboarding completes.
 * @param createdAtMillis Wall-clock time the nickname was first saved; set once, never overwritten.
 * @param hasCompletedFirstLaunch Drives Welcome-vs-Home routing in the splash screen. Tracked as
 * its own flag rather than inferred from `nickname.isNotEmpty()` so a future phase that allows
 * clearing/editing the nickname can't accidentally re-trigger onboarding.
 * @param reserved Free-form slot for future profile fields (e.g. avatar, preferences summary)
 * without reshaping this data class; intentionally never persisted to DataStore since nothing
 * populates it yet — see [com.behaviorengine.core.domain.engine.EngineState.reserved] for the
 * same pattern.
 */
data class UserProfile(
    val nickname: String = "",
    val createdAtMillis: Long = 0L,
    val hasCompletedFirstLaunch: Boolean = false,
    val reserved: Map<String, String> = emptyMap()
)
