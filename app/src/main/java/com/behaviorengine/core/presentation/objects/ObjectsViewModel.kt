package com.behaviorengine.core.presentation.objects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.objects.VisualObject
import com.behaviorengine.core.domain.objects.VisualObjectRepository
import com.behaviorengine.core.domain.objects.VisualObjectStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val OBJECTS_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Drives [ObjectsScreen]: the reactive, searched object list, and the three-dot menu actions
 * (edit/disable-or-enable/delete). Object creation has no form yet — [onCreateObjectClicked]
 * gives the new object a placeholder name and immediately hands off navigation to its details
 * screen, matching this phase's "no image processing, only object management" scope.
 *
 * `visibleObjects` is a plain `StateFlow<List<VisualObject>>` rather than an
 * `ImmutableList`-backed one; [VisualObject] itself is `@Immutable` and `LazyColumn` uses a
 * stable `key`, which covers this phase's actual (near-empty) scale. Reaching for
 * `kotlinx.collections.immutable` is a real option if a future phase's library grows large enough
 * for it to matter, not a default to apply pre-emptively.
 */
@HiltViewModel
class ObjectsViewModel @Inject constructor(
    private val repository: VisualObjectRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val visibleObjects: StateFlow<List<VisualObject>> = combine(
        repository.objects,
        _searchQuery
    ) { objects, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) objects else objects.filter { it.name.contains(trimmed, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(OBJECTS_SUBSCRIPTION_TIMEOUT_MILLIS), emptyList())

    private val _navigateToObjectId = MutableSharedFlow<String>()
    val navigateToObjectId: SharedFlow<String> = _navigateToObjectId.asSharedFlow()

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCreateObjectClicked() {
        viewModelScope.launch {
            val ordinal = repository.objects.value.size + 1
            val created = repository.createObject(name = "Visual Object #$ordinal")
            _navigateToObjectId.emit(created.id)
        }
    }

    fun onToggleEnabledClicked(target: VisualObject) {
        viewModelScope.launch {
            val newStatus = if (target.status == VisualObjectStatus.DISABLED) {
                VisualObjectStatus.READY
            } else {
                VisualObjectStatus.DISABLED
            }
            repository.updateObject(target.copy(status = newStatus))
        }
    }

    fun onDeleteConfirmed(objectId: String) {
        viewModelScope.launch {
            repository.deleteObject(objectId)
        }
    }
}
