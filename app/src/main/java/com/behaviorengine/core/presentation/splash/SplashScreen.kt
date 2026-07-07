package com.behaviorengine.core.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.behaviorengine.core.common.AppConstants
import kotlinx.coroutines.delay

private const val SPLASH_DISPLAY_MILLIS = 800L

/**
 * Brief branding screen shown once at cold start before landing on Home. Not a functional
 * screen per this phase's spec — it exists only to prove the navigation graph itself works,
 * ahead of any real splash-time initialization (engine warm-up, settings load) landing here.
 */
@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DISPLAY_MILLIS)
        onSplashFinished()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppConstants.PROJECT_NAME,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
