package com.behaviorengine.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BehaviorEngineColorScheme = darkColorScheme(
    primary = AccentPrimary,
    onPrimary = AccentOnPrimary,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    error = StatusError
)

/**
 * App-wide Compose theme. Always dark per this phase's spec; the system theme is intentionally
 * not consulted. [com.behaviorengine.settings.ThemeMode] reserves the model for a future
 * light/system option without requiring this function's signature to change.
 */
@Composable
fun BehaviorEngineTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BehaviorEngineColorScheme,
        typography = Typography,
        content = content
    )
}
