package com.behaviorengine.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.behaviorengine.core.presentation.automation.AutomationScreen
import com.behaviorengine.core.presentation.engine.EngineScreen
import com.behaviorengine.core.presentation.home.HomeScreen
import com.behaviorengine.core.presentation.objects.ObjectsScreen
import com.behaviorengine.core.presentation.settings.SettingsScreen
import com.behaviorengine.core.presentation.splash.SplashScreen
import com.behaviorengine.core.presentation.teaching.TeachingScreen
import com.behaviorengine.core.presentation.welcome.WelcomeScreen

private const val TRANSITION_MILLIS = 300
private const val SLIDE_DIVISOR = 5

/**
 * Top-level navigation graph. Only [Screen.Home] and [Screen.Welcome] are fully designed this
 * phase; [Screen.Objects]/[Screen.Teaching]/[Screen.Automation]/[Screen.Settings] are simple
 * placeholders. [Screen.EngineDiagnostics] is intentionally not part of the main flow — see
 * [EngineScreen]'s KDoc.
 *
 * Every destination shares one subtle fade+slide transition (applied once at the [NavHost] level
 * rather than per `composable()` call) per this phase's "keep animations subtle" spec.
 */
@Composable
fun BehaviorEngineNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        enterTransition = {
            fadeIn(tween(TRANSITION_MILLIS)) + slideInHorizontally(tween(TRANSITION_MILLIS)) { it / SLIDE_DIVISOR }
        },
        exitTransition = { fadeOut(tween(TRANSITION_MILLIS)) },
        popEnterTransition = { fadeIn(tween(TRANSITION_MILLIS)) },
        popExitTransition = {
            fadeOut(tween(TRANSITION_MILLIS)) + slideOutHorizontally(tween(TRANSITION_MILLIS)) { it / SLIDE_DIVISOR }
        }
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onDestinationDetermined = { destination ->
                    val target = if (destination == StartDestination.Home) Screen.Home.route else Screen.Welcome.route
                    navController.navigate(target) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onOnboardingComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToObjects = { navController.navigate(Screen.Objects.route) },
                onNavigateToTeaching = { navController.navigate(Screen.Teaching.route) },
                onNavigateToAutomation = { navController.navigate(Screen.Automation.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        composable(Screen.Objects.route) {
            ObjectsScreen()
        }
        composable(Screen.Teaching.route) {
            TeachingScreen()
        }
        composable(Screen.Automation.route) {
            AutomationScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onEngineDiagnosticsClick = { navController.navigate(Screen.EngineDiagnostics.route) }
            )
        }
        composable(Screen.EngineDiagnostics.route) {
            EngineScreen(onBackClick = { navController.popBackStack() })
        }
    }
}
