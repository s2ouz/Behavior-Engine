package com.behaviorengine.settings

import kotlinx.serialization.Serializable

/**
 * User-facing preference model backing the Settings screen. `@Serializable` even though nothing
 * serializes it yet: kotlinx.serialization is this project's chosen format for any future
 * settings export/import or remote sync, so the model is shaped for that from day one.
 *
 * Persistence (DataStore) is intentionally not wired up yet — see [SettingsManager].
 */
@Serializable
data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val debugModeEnabled: Boolean = false,
    val loggingEnabled: Boolean = true,
    /** Reserved for future phases: model selection, confidence thresholds, on-device vs remote. */
    val aiSettings: AiSettings = AiSettings(),
    /** Reserved for future phases: enabled accessibility features, target app allow-list. */
    val accessibilitySettings: AccessibilitySettings = AccessibilitySettings()
)

/** Only [DARK] is implemented in the UI today; the enum exists so Settings can offer more later. */
@Serializable
enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

/** Empty on purpose — no AI exists yet. Prepared so [AppSettings] doesn't need reshaping later. */
@Serializable
data class AiSettings(
    val placeholder: Boolean = false
)

/** Empty on purpose — no accessibility service exists yet. Same rationale as [AiSettings]. */
@Serializable
data class AccessibilitySettings(
    val placeholder: Boolean = false
)
