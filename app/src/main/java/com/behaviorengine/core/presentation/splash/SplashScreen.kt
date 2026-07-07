package com.behaviorengine.core.presentation.splash

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.core.common.AppConstants
import com.behaviorengine.navigation.StartDestination
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

private const val MINIMUM_DISPLAY_MILLIS = 800L

/**
 * Brief branding screen shown once at cold start. Now does real work: waits for
 * [SplashViewModel] to determine [StartDestination] (first launch vs. returning user) and for a
 * minimum branding display time, whichever takes longer, then navigates exactly once.
 */
@Composable
fun SplashScreen(viewModel: SplashViewModel = hiltViewModel(), onDestinationDetermined: (StartDestination) -> Unit) {
    LaunchedEffect(Unit) {
        val minimumDisplay = async { delay(MINIMUM_DISPLAY_MILLIS) }
        val destination = viewModel.startDestination.filterNotNull().first()
        minimumDisplay.await()
        onDestinationDetermined(destination)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppConstants.PROJECT_NAME,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
