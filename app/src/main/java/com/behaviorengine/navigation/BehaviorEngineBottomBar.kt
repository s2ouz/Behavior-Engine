package com.behaviorengine.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.behaviorengine.R

private data class BottomNavDestination(val screen: Screen, val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_NAV_DESTINATIONS = listOf(
    BottomNavDestination(Screen.Objects, R.string.nav_objects, Icons.Filled.Collections),
    BottomNavDestination(Screen.Teaching, R.string.nav_teaching, Icons.Filled.School),
    BottomNavDestination(Screen.Automation, R.string.nav_automation, Icons.Filled.PlayArrow),
    BottomNavDestination(Screen.Settings, R.string.nav_settings, Icons.Filled.Settings)
)

/**
 * The four-tab bottom bar (Objects/Teaching/Automation/Settings) shown for exactly the routes in
 * [Screen.BOTTOM_NAV_ROUTES] — [BehaviorEngineNavGraph] decides visibility, this composable only
 * renders the bar itself.
 */
@Composable
fun BehaviorEngineBottomBar(currentRoute: String?, onNavigate: (Screen) -> Unit) {
    NavigationBar {
        BOTTOM_NAV_DESTINATIONS.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.screen.route,
                onClick = { onNavigate(destination.screen) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) }
            )
        }
    }
}
