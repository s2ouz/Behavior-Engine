package com.behaviorengine.core.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.behaviorengine.core.domain.objects.VisualObjectStatus

/** A small rounded pill showing a [VisualObjectStatus]'s label in its status color. */
@Composable
fun StatusBadge(status: VisualObjectStatus, modifier: Modifier = Modifier) {
    val color = status.displayColor()
    Text(
        text = status.displayLabel(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier
            .background(color = color.copy(alpha = BADGE_BACKGROUND_ALPHA), shape = RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private const val BADGE_BACKGROUND_ALPHA = 0.16f
