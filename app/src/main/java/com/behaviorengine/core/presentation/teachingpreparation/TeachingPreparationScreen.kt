package com.behaviorengine.core.presentation.teachingpreparation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.ui.theme.StatusRunning

private const val PROGRESS_INDICATOR_SIZE_DP = 96
private const val PROGRESS_INDICATOR_STROKE_WIDTH_DP = 6
private const val DISABLED_ITEM_ALPHA = 0.5f

/**
 * "System prepares" per this phase's UX goal — a large indeterminate spinner doubles as both the
 * spec's "Large Icon" and its "Loading animation" without inventing separate assets. Nothing here
 * captures anything: [com.behaviorengine.core.domain.teaching.TeachingManager.startSession] has
 * already moved the session to `PREPARING` by the time this screen shows, and it stays there
 * until [TeachingPreparationViewModel.onFinishClicked] moves it to `FINISHED`.
 */
@Composable
fun TeachingPreparationScreen(
    viewModel: TeachingPreparationViewModel = hiltViewModel(),
    onFinishClick: () -> Unit
) {
    TeachingPreparationContent(
        onFinishClick = {
            viewModel.onFinishClicked()
            onFinishClick()
        }
    )
}

@Composable
private fun TeachingPreparationContent(onFinishClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(PROGRESS_INDICATOR_SIZE_DP.dp),
                strokeWidth = PROGRESS_INDICATOR_STROKE_WIDTH_DP.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.teaching_preparation_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(32.dp))

            ChecklistItem(checked = true, label = stringResource(R.string.teaching_preparation_checklist_object_selected))
            Spacer(modifier = Modifier.height(12.dp))
            ChecklistItem(checked = true, label = stringResource(R.string.teaching_preparation_checklist_session_created))
            Spacer(modifier = Modifier.height(12.dp))
            ChecklistItem(checked = false, label = stringResource(R.string.teaching_preparation_checklist_capture_permission))
            Spacer(modifier = Modifier.height(12.dp))
            ChecklistItem(checked = false, label = stringResource(R.string.teaching_preparation_checklist_capture_engine))
        }

        Button(
            onClick = onFinishClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(stringResource(R.string.teaching_preparation_finish_button))
        }
    }
}

@Composable
private fun ChecklistItem(checked: Boolean, label: String) {
    val contentColor = if (checked) {
        MaterialTheme.colorScheme.onBackground
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DISABLED_ITEM_ALPHA)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (checked) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) StatusRunning else contentColor
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge, color = contentColor)
    }
}
