package com.behaviorengine.core.common

import com.behaviorengine.BuildConfig

/**
 * Single source of truth for values that would otherwise be hardcoded/duplicated across
 * screens, managers and logs (app name in a Toast here, version number in a screen there...).
 * Anything that changes once per release or per phase belongs here, not inline.
 */
object AppConstants {

    const val PROJECT_NAME: String = "Behavior Engine"

    /** App package version, sourced from Gradle so it can never drift from build.gradle.kts. */
    val APP_VERSION: String = BuildConfig.VERSION_NAME

    /**
     * Version of the engine core itself, tracked separately from [APP_VERSION] because the
     * engine (vision/behavior/automation) is expected to version independently of the
     * surrounding app shell as it matures.
     */
    const val ENGINE_VERSION: String = "0.1.0"

    /** Human-readable label for the development phase currently being built, shown in the UI. */
    const val CURRENT_PHASE: String = "Foundation"

    /** True only in debug builds; gates verbose logging and future debug-only UI affordances. */
    val DEBUG_MODE: Boolean = BuildConfig.DEBUG
}
