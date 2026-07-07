package com.behaviorengine.core.presentation.objectdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.domain.objects.VisualObjectRepository
import com.behaviorengine.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val DETAILS_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Reads the single [VisualObject] this screen was navigated to for, by id from
 * [SavedStateHandle] (the standard Hilt+Navigation-Compose pattern for nav arguments). Reactive
 * rather than a one-shot load: if the object is edited or deleted elsewhere while this screen is
 * visible, it reflects that immediately — including becoming `null` if deleted, which
 * `ObjectDetailsScreen` handles by showing a "not found" message rather than crashing.
 */
@HiltViewModel
class ObjectDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: VisualObjectRepository
) : ViewModel() {

    private val objectId: String = checkNotNull(savedStateHandle[Screen.ObjectDetails.ARG_OBJECT_ID])

    val visualObject: StateFlow<VisualObject?> = repository.objects
        .map { objects -> objects.firstOrNull { it.id == objectId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(DETAILS_SUBSCRIPTION_TIMEOUT_MILLIS), null)
}
