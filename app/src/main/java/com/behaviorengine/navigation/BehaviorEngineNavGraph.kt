package com.behaviorengine.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.behaviorengine.core.presentation.automation.AutomationScreen
import com.behaviorengine.core.presentation.engine.EngineScreen
import com.behaviorengine.core.presentation.objectdetails.ObjectDetailsScreen
import com.behaviorengine.core.presentation.objects.ObjectsScreen
import com.behaviorengine.core.presentation.settings.SettingsScreen
import com.behaviorengine.core.presentation.splash.SplashScreen
import com.behaviorengine.core.presentation.teaching.TeachingScreen
import com.behaviorengine.core.presentation.teachingpreparation.TeachingPreparationScreen
import com.behaviorengine.core.presentation.welcome.WelcomeScreen

private const val TRANSITION_MILLIS = 300
private const val SLIDE_DIVISOR = 5

/** Returns to the Objects tab exactly as tapping its bottom nav item would — same save/restore semantics. */
private fun navigateToObjectsTab(navController: NavHostController) {
    navController.navigate(Screen.Objects.route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Top-level navigation graph. A [Scaffold] with a bottom bar wraps the whole [NavHost]; the bar
 * itself only renders for [Screen.BOTTOM_NAV_ROUTES] — Splash, Welcome, [Screen.ObjectDetails],
 * [Screen.EngineDiagnostics], and [Screen.TeachingPreparation] are all full-screen with no bottom
 * bar, since they're either pre-onboarding or "detail"/"in-flow" screens one level below the four
 * main tabs.
 *
 * Only [Screen.Objects] and [Screen.Welcome] are fully designed this phase;
 * [Screen.Teaching]/[Screen.Automation]/[Screen.Settings] remain simple placeholders.
 */
@Composable
fun BehaviorEngineNavGraph(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Screen.BOTTOM_NAV_ROUTES

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BehaviorEngineBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { destination ->
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding),
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
                        val target = if (destination == StartDestination.Objects) Screen.Objects.route else Screen.Welcome.route
                        navController.navigate(target) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Welcome.route) {
                WelcomeScreen(
                    onOnboardingComplete = {
                        navController.navigate(Screen.Objects.route) {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Objects.route) {
                ObjectsScreen(
                    onObjectSelected = { objectId ->
                        navController.navigate(Screen.ObjectDetails.createRoute(objectId))
                    }
                )
            }
            composable(Screen.Teaching.route) {
                TeachingScreen(
                    onStartTeaching = { sessionId ->
                        navController.navigate(Screen.TeachingPreparation.createRoute(sessionId))
                    },
                    onCancelClick = { navigateToObjectsTab(navController) }
                )
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
            composable(
                route = Screen.ObjectDetails.route,
                arguments = listOf(navArgument(Screen.ObjectDetails.ARG_OBJECT_ID) { type = NavType.StringType })
            ) {
                ObjectDetailsScreen(onBackClick = { navController.popBackStack() })
            }
            composable(
                route = Screen.TeachingPreparation.route,
                arguments = listOf(navArgument(Screen.TeachingPreparation.ARG_SESSION_ID) { type = NavType.StringType })
            ) {
                TeachingPreparationScreen(onFinishClick = { navigateToObjectsTab(navController) })
            }
        }
    }
}
