package com.behaviorengine.core.presentation.teaching

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.presentation.common.StatusBadge

private const val SECTION_CARD_CORNER_RADIUS_DP = 16

/**
 * Entry point of the teaching workflow. No capture, no recognition — this phase only lets the
 * user start a [com.behaviorengine.core.domain.teaching.TeachingSession] and see it through to
 * [TeachingPreparationScreen][com.behaviorengine.core.presentation.teachingpreparation.TeachingPreparationScreen].
 */
@Composable
fun TeachingScreen(
    viewModel: TeachingViewModel = hiltViewModel(),
    onStartTeaching: (String) -> Unit,
    onCancelClick: () -> Unit
) {
    val selectedObject by viewModel.selectedObject.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToSessionId.collect { sessionId -> onStartTeaching(sessionId) }
    }

    TeachingContent(
        selectedObject = selectedObject,
        onStartClick = viewModel::onStartTeachingClicked,
        onCancelClick = onCancelClick
    )
}

@Composable
private fun TeachingContent(
    selectedObject: VisualObject?,
    onStartClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.teaching_screen_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.teaching_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.teaching_selected_object_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        SelectedObjectCard(selectedObject)

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.teaching_info_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.teaching_info_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onStartClick,
            enabled = selectedObject != null,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.teaching_start_button))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.teaching_cancel_button))
        }
    }
}

@Composable
private fun SelectedObjectCard(selectedObject: VisualObject?) {
    Card(
        shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedObject == null) {
                Text(
                    text = stringResource(R.string.teaching_no_object_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.teaching_no_object_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Text(
                text = selectedObject.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = selectedObject.status)
                Text(
                    text = pluralStringResource(
                        R.plurals.objects_image_count,
                        selectedObject.imageCount,
                        selectedObject.imageCount
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
