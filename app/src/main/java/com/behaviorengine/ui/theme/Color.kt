package com.behaviorengine.ui.theme

import androidx.compose.ui.graphics.Color

// Dark-only palette per the Foundation phase's UI spec. Named by role, not by hue, so a future
// light/system theme (see settings.ThemeMode) can reuse these names with different values.
val BackgroundDark = Color(0xFF0E0E12)
val SurfaceDark = Color(0xFF1A1A20)
val OnSurfaceDark = Color(0xFFE6E6EB)
val OnSurfaceVariantDark = Color(0xFF9A9AA5)
val AccentPrimary = Color(0xFF6C63FF)
val AccentOnPrimary = Color(0xFFFFFFFF)
val StatusRunning = Color(0xFF4CD787)
val StatusError = Color(0xFFFF5C5C)
val StatusIdle = Color(0xFF9A9AA5)

/** Yellow — the fourth of exactly four status colors this phase's spec allows (green/yellow/gray/red). */
val StatusTraining = Color(0xFFF5C453)
