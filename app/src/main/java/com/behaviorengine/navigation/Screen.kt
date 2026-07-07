package com.behaviorengine.navigation

/**
 * Type-safe route definitions for [BehaviorEngineNavGraph]. A sealed class rather than raw
 * string literals so an invalid route is a compile error, not a runtime crash on navigate().
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Home : Screen("home")
    data object Objects : Screen("objects")
    data object Teaching : Screen("teaching")
    data object Automation : Screen("automation")
    data object Settings : Screen("settings")

    /** Not part of the product's main flow — reachable only from [Settings]; see EngineScreen's KDoc. */
    data object EngineDiagnostics : Screen("engine_diagnostics")
}
