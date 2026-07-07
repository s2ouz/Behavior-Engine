package com.behaviorengine.core.presentation.objects

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.behaviorengine.R
import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.domain.objects.VisualObjectStatus
import com.behaviorengine.core.presentation.common.StatusBadge
import com.behaviorengine.utils.TimeFormatter

private const val CARD_CORNER_RADIUS_DP = 16
private const val CARD_ELEVATION_DP = 2
private const val CARD_PRESSED_ELEVATION_DP = 6

/**
 * One row in `ObjectsScreen`'s `LazyColumn`. The card's own press-elevation animation is
 * Material3's built-in behavior (`CardDefaults.cardElevation`'s `pressedElevation`) — this
 * phase's "small card elevation animation" spec point needs no manual animation code.
 */
@Composable
fun ObjectCard(
    visualObject: VisualObject,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onToggleEnabledClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CARD_ELEVATION_DP.dp,
            pressedElevation = CARD_PRESSED_ELEVATION_DP.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = visualObject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusBadge(status = visualObject.status)
                    Text(
                        text = pluralStringResource(
                            R.plurals.objects_image_count,
                            visualObject.imageCount,
                            visualObject.imageCount
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.objects_created_date, TimeFormatter.formatDate(visualObject.createdAtMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.objects_card_menu_description))
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.objects_menu_edit)) },
                    leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onEditClick()
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (visualObject.status == VisualObjectStatus.DISABLED) {
                                    R.string.objects_menu_enable
                                } else {
                                    R.string.objects_menu_disable
                                }
                            )
                        )
                    },
                    leadingIcon = {
                        Icon(
                            if (visualObject.status == VisualObjectStatus.DISABLED) Icons.Filled.CheckCircle else Icons.Filled.Block,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleEnabledClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.objects_menu_delete)) },
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                    onClick = {
                        menuExpanded = false
                        onDeleteClick()
                    }
                )
            }
        }
    }
}
