package com.behaviorengine.navigation

/**
 * Type-safe route definitions for [BehaviorEngineNavGraph]. A sealed class rather than raw
 * string literals so an invalid route is a compile error, not a runtime crash on navigate().
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Settings : Screen("settings")
}
