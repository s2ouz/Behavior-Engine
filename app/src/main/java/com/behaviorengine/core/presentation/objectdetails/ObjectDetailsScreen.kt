package com.behaviorengine.core.presentation.objectdetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.presentation.common.InfoRow
import com.behaviorengine.core.presentation.common.StatusBadge
import com.behaviorengine.utils.TimeFormatter

/**
 * Read-only view of a single [VisualObject] — "No editing yet" per this phase's spec; a future
 * phase adds the ability to rename, add notes, or manage images from here.
 */
@Composable
fun ObjectDetailsScreen(viewModel: ObjectDetailsViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val visualObject by viewModel.visualObject.collectAsState()

    ObjectDetailsContent(visualObject = visualObject, onBackClick = onBackClick)
}

@Composable
private fun ObjectDetailsContent(visualObject: VisualObject?, onBackClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(16.dp)) {
            TextButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.object_details_back_button))
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (visualObject == null) {
                Text(
                    text = stringResource(R.string.object_details_not_found),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = visualObject.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            StatusBadge(status = visualObject.status)

            Spacer(modifier = Modifier.height(32.dp))

            InfoRow(
                label = stringResource(R.string.object_details_image_count_label),
                value = visualObject.imageCount.toString()
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.object_details_created_label),
                value = TimeFormatter.formatDate(visualObject.createdAtMillis)
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(
                label = stringResource(R.string.object_details_recognition_label),
                value = stringResource(
                    if (visualObject.recognitionEnabled) {
                        R.string.object_details_recognition_enabled
                    } else {
                        R.string.object_details_recognition_disabled
                    }
                )
            )

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.object_details_future_ai_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.object_details_future_ai_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
