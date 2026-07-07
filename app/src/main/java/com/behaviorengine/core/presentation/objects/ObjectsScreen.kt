package com.behaviorengine.core.presentation.objects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.domain.objects.VisualObject

private const val CONTENT_FADE_MILLIS = 400

/**
 * The heart of the product: the taught-object library. Landing destination straight out of
 * onboarding per this phase's UX goal — "the user should immediately understand: I will build my
 * own visual library here," whether or not any objects exist yet.
 */
@Composable
fun ObjectsScreen(
    viewModel: ObjectsViewModel = hiltViewModel(),
    onObjectSelected: (String) -> Unit
) {
    val objects by viewModel.visibleObjects.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigateToObjectId.collect { objectId -> onObjectSelected(objectId) }
    }

    ObjectsContent(
        objects = objects,
        searchQuery = searchQuery,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onCreateClicked = viewModel::onCreateObjectClicked,
        onObjectClicked = onObjectSelected,
        onToggleEnabledClicked = viewModel::onToggleEnabledClicked,
        onDeleteConfirmed = viewModel::onDeleteConfirmed
    )
}

@Composable
private fun ObjectsContent(
    objects: List<VisualObject>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onCreateClicked: () -> Unit,
    onObjectClicked: (String) -> Unit,
    onToggleEnabledClicked: (VisualObject) -> Unit,
    onDeleteConfirmed: (String) -> Unit
) {
    var pendingDeletion by remember { mutableStateOf<VisualObject?>(null) }

    Scaffold(
        floatingActionButton = {
            if (objects.isNotEmpty()) {
                FloatingActionButton(onClick = onCreateClicked) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.objects_new_object_button))
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.objects_screen_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.objects_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.objects_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = true, enter = fadeIn(tween(CONTENT_FADE_MILLIS))) {
                if (objects.isEmpty()) {
                    EmptyObjectsView(onCreateClick = onCreateClicked)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 96.dp)
                    ) {
                        items(objects, key = { it.id }) { visualObject ->
                            ObjectCard(
                                visualObject = visualObject,
                                onClick = { onObjectClicked(visualObject.id) },
                                onEditClick = { onObjectClicked(visualObject.id) },
                                onToggleEnabledClick = { onToggleEnabledClicked(visualObject) },
                                onDeleteClick = { pendingDeletion = visualObject }
                            )
                        }
                    }
                }
            }
        }
    }

    pendingDeletion?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeletion = null },
            title = { Text(stringResource(R.string.objects_delete_dialog_title)) },
            text = { Text(stringResource(R.string.objects_delete_dialog_message, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirmed(target.id)
                    pendingDeletion = null
                }) {
                    Text(stringResource(R.string.objects_delete_dialog_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletion = null }) {
                    Text(stringResource(R.string.objects_delete_dialog_cancel))
                }
            }
        )
    }
}
