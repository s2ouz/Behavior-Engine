package com.behaviorengine.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.behaviorengine.core.presentation.home.HomeScreen
import com.behaviorengine.core.presentation.settings.SettingsScreen
import com.behaviorengine.core.presentation.splash.SplashScreen

/**
 * Top-level navigation graph. Only [Screen.Home] is functional this phase; [Screen.Settings]
 * is a placeholder and [Screen.Splash] exists purely to prove the graph itself works.
 */
@Composable
fun BehaviorEngineNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
