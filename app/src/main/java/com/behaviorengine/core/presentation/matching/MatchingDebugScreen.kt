package com.behaviorengine.core.presentation.matching

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.behaviorengine.R
import com.behaviorengine.core.domain.matching.MatchResult
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import com.behaviorengine.core.presentation.common.InfoRow

private const val SECTION_CARD_CORNER_RADIUS_DP = 16

/**
 * Development/debugging screen for SPEC-11's Intelligent Visual Matching Engine — not part of the
 * product's main flow, reachable only from Settings, same reasoning as `EngineScreen`. Lets a
 * developer start the engine's own screen capture, pick any taught object, and see exactly what
 * [com.behaviorengine.core.domain.matching.VisualMatchingManager] finds (or doesn't) with full
 * confidence/scale/method detail — the "debugging screen" and "debug overlay" the spec calls for.
 */
@Composable
fun MatchingDebugScreen(viewModel: MatchingDebugViewModel = hiltViewModel(), onBackClick: () -> Unit) {
    val context = LocalContext.current
    val isCaptureActive by viewModel.isCaptureActive.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val templates by viewModel.templates.collectAsState()
    val lastResult by viewModel.lastResult.collectAsState()
    val processingTimeMillis by viewModel.lastProcessingTimeMillis.collectAsState()
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val searchedTemplateId by viewModel.searchedTemplateId.collectAsState()

    var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }
    var hasNotificationPermission by remember { mutableStateOf(hasNotificationPermission(context)) }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null) {
            permissionDeniedMessage = null
            viewModel.onCaptureGranted(result.resultCode, data)
        } else {
            permissionDeniedMessage = context.getString(R.string.matching_debug_permission_projection_denied)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { viewModel.onOverlayToggled(Settings.canDrawOverlays(context)) }

    fun onStartCaptureClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            mediaProjectionLauncher.launch(viewModel.createCaptureIntent())
        }
    }

    fun onOverlaySwitchToggled(enabled: Boolean) {
        if (enabled && !Settings.canDrawOverlays(context)) {
            overlayPermissionLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
            )
        } else {
            viewModel.onOverlayToggled(enabled)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onBackClick) {
                Text(stringResource(R.string.matching_debug_back_button))
            }
        }

        Text(
            text = stringResource(R.string.matching_debug_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.matching_debug_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isCaptureActive) {
            Card(
                shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text(
                    text = stringResource(R.string.matching_debug_capture_inactive),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            permissionDeniedMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = ::onStartCaptureClick, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.matching_debug_start_capture_button))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.matching_debug_overlay_toggle), style = MaterialTheme.typography.bodyMedium)
                Switch(checked = overlayEnabled, onCheckedChange = ::onOverlaySwitchToggled)
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = viewModel::onStopCaptureClicked, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.matching_debug_stop_capture_button))
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (templates.isEmpty()) {
                Text(
                    text = stringResource(R.string.matching_debug_no_templates),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateRow(
                            template = template,
                            isSearching = isSearching && searchedTemplateId == template.id,
                            onFindClick = { viewModel.onFindClicked(template.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            ResultCard(result = lastResult, processingTimeMillis = processingTimeMillis, searchedTemplateId = searchedTemplateId)
        }
    }
}

@Composable
private fun TemplateRow(template: ObjectTemplate, isSearching: Boolean, onFindClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = template.id.take(8), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${template.width}x${template.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSearching) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp))
            } else {
                Button(onClick = onFindClick) {
                    Text(stringResource(R.string.matching_debug_find_button))
                }
            }
        }
    }
}

@Composable
private fun ResultCard(result: MatchResult?, processingTimeMillis: Long, searchedTemplateId: String?) {
    if (searchedTemplateId == null) return
    Card(
        shape = RoundedCornerShape(SECTION_CARD_CORNER_RADIUS_DP.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (result == null) {
                Text(
                    text = stringResource(R.string.matching_debug_no_match),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InfoRow(stringResource(R.string.matching_debug_result_confidence), "${result.confidence}%")
                InfoRow(stringResource(R.string.matching_debug_result_quality), result.quality.name)
                InfoRow(stringResource(R.string.matching_debug_result_scale), "${result.scale}x")
                InfoRow(stringResource(R.string.matching_debug_result_method), result.method)
            }
            InfoRow(stringResource(R.string.matching_debug_result_processing_time), "${processingTimeMillis}ms")
        }
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
