package com.behaviorengine.core.presentation.teaching

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.behaviorengine.R
import com.behaviorengine.core.domain.teaching.TeachingSession
import com.behaviorengine.core.domain.teaching.TeachingState
import com.behaviorengine.core.presentation.common.InfoRow
import com.behaviorengine.utils.TimeFormatter
import kotlinx.coroutines.delay

private const val SECTION_CARD_CORNER_RADIUS_DP = 16
private const val TICKER_INTERVAL_MILLIS = 500L

/**
 * Teaching Mode's entry point. Two states: idle (explain + Start Teaching, gated by whichever
 * permission is still missing) and active (Pause/Resume/Finish/Cancel + live stats) — no
 * technical terms, per this phase's UX goal.
 */
@Composable
fun TeachingScreen(viewModel: TeachingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val session by viewModel.currentSession.collectAsState()
    val state by viewModel.currentState.collectAsState()

    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember { mutableStateOf(hasNotificationPermission(context)) }
    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            permissionDeniedMessage = null
            viewModel.onMediaProjectionGranted(result.resultCode, data)
        } else {
            permissionDeniedMessage = context.getString(R.string.teaching_permission_projection_denied)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    fun onStartTeachingClick() {
        when {
            !hasOverlayPermission -> {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission -> {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> mediaProjectionLauncher.launch(viewModel.createCaptureIntent())
        }
    }

    val isActive = state == TeachingState.PREPARING || state == TeachingState.RECORDING || state == TeachingState.PAUSED

    if (isActive && session != null) {
        ActiveTeachingContent(
            session = requireNotNull(session),
            state = state,
            storageUsedBytes = viewModel::storageUsedBytes,
            onPauseClick = viewModel::onPauseClicked,
            onResumeClick = viewModel::onResumeClicked,
            onFinishClick = viewModel::onFinishClicked,
            onCancelClick = viewModel::onCancelClicked
        )
    } else {
        IdleTeachingContent(
            hasOverlayPermission = hasOverlayPermission,
            hasNotificationPermission = hasNotificationPermission,
            permissionDeniedMessage = permissionDeniedMessage,
            onStartClick = ::onStartTeachingClick
        )
    }
}

@Composable
private fun IdleTeachingContent(
    hasOverlayPermission: Boolean,
    hasNotificationPermission: Boolean,
    permissionDeniedMessage: String?,
    onStartClick: () -> Unit
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

        InfoCard(stringResource(R.string.teaching_info_description))

        Spacer(modifier = Modifier.height(16.dp))

        if (!hasOverlayPermission) {
            InfoCard(stringResource(R.string.teaching_permission_overlay_explanation))
            Spacer(modifier = Modifier.height(16.dp))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            InfoCard(stringResource(R.string.teaching_permission_notification_explanation))
            Spacer(modifier = Modifier.height(16.dp))
        }

        permissionDeniedMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = onStartClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.teaching_start_button))
        }
    }
}

@Composable
private fun ActiveTeachingContent(
    session: TeachingSession,
    state: TeachingState,
    storageUsedBytes: () -> Long,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onFinishClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    var elapsedMillis by remember(session.id) { mutableLongStateOf(0L) }
    LaunchedEffect(session.id, session.startedAtMillis, state) {
        while (true) {
            val startedAt = session.startedAtMillis
            if (startedAt != null && state == TeachingState.RECORDING) {
                elapsedMillis = System.currentTimeMillis() - startedAt
            }
            delay(TICKER_INTERVAL_MILLIS)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.teaching_active_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InfoRow(stringResource(R.string.teaching_stat_current_app), session.applicationName)
                InfoRow(stringResource(R.string.teaching_stat_recording_time), TimeFormatter.formatElapsed(elapsedMillis))
                InfoRow(stringResource(R.string.teaching_stat_touch_count), session.touchCount.toString())
                InfoRow(stringResource(R.string.teaching_stat_frame_count), session.frameCount.toString())
                InfoRow(stringResource(R.string.teaching_stat_resolution), "${session.screenWidth}x${session.screenHeight}")
                InfoRow(stringResource(R.string.teaching_stat_orientation), session.orientation)
                InfoRow(stringResource(R.string.teaching_stat_storage_used), formatBytes(storageUsedBytes()))
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state == TeachingState.PAUSED) {
            Button(onClick = onResumeClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.teaching_resume_button))
            }
        } else {
            Button(onClick = onPauseClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.teaching_pause_button))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = onFinishClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.teaching_finish_button))
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.teaching_cancel_button))
        }
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    return "%.1f MB".format(mb)
}
