package com.behaviorengine.navigation

private const val OBJECT_DETAILS_ARG_OBJECT_ID = "objectId"
private const val TEACHING_PREPARATION_ARG_SESSION_ID = "sessionId"

/**
 * Type-safe route definitions for [BehaviorEngineNavGraph]. A sealed class rather than raw
 * string literals so an invalid route is a compile error, not a runtime crash on navigate().
 *
 * There is no `Home` route as of v0.7.0: the bottom navigation bar (Objects/Teaching/Automation/
 * Settings) replaced the v0.6.0 card-based hub entirely, since a persistent bottom bar is a
 * strictly better fit for "the user should land in Objects, not a menu screen first."
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Welcome : Screen("welcome")
    data object Objects : Screen("objects")
    data object Teaching : Screen("teaching")
    data object Automation : Screen("automation")
    data object Settings : Screen("settings")

    /** Not part of the product's main flow — reachable only from [Settings]; see EngineScreen's KDoc. */
    data object EngineDiagnostics : Screen("engine_diagnostics")

    /** Takes a [VisualObject][com.behaviorengine.core.domain.objects.VisualObject] id argument. */
    data object ObjectDetails : Screen("object_details/{$OBJECT_DETAILS_ARG_OBJECT_ID}") {
        const val ARG_OBJECT_ID = OBJECT_DETAILS_ARG_OBJECT_ID
        fun createRoute(objectId: String) = "object_details/$objectId"
    }

    /** Takes a [TeachingSession][com.behaviorengine.core.domain.teaching.TeachingSession] id argument. */
    data object TeachingPreparation : Screen("teaching_preparation/{$TEACHING_PREPARATION_ARG_SESSION_ID}") {
        const val ARG_SESSION_ID = TEACHING_PREPARATION_ARG_SESSION_ID
        fun createRoute(sessionId: String) = "teaching_preparation/$sessionId"
    }

    companion object {
        /** Destinations the bottom navigation bar shows itself for; see `BehaviorEngineBottomBar`. */
        val BOTTOM_NAV_ROUTES = setOf(Objects.route, Teaching.route, Automation.route, Settings.route)
    }
}
