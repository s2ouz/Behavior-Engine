package com.behaviorengine.core.presentation.matching

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.matching.DebugOverlayInfo
import com.behaviorengine.core.domain.matching.DebugOverlayManager
import com.behaviorengine.core.domain.matching.MatchResult
import com.behaviorengine.core.domain.matching.VisualMatchingManager
import com.behaviorengine.core.domain.objectlearning.ObjectRepository
import com.behaviorengine.core.domain.objectlearning.ObjectTemplate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives [MatchingDebugScreen]. Deliberately thin — every action is a one-line delegation to
 * [VisualMatchingManager] or [DebugOverlayManager], mirroring [com.behaviorengine.core.presentation.teaching.TeachingViewModel]'s role.
 */
@HiltViewModel
class MatchingDebugViewModel @Inject constructor(
    private val visualMatchingManager: VisualMatchingManager,
    private val objectRepository: ObjectRepository,
    private val debugOverlayManager: DebugOverlayManager
) : ViewModel() {

    val isCaptureActive: StateFlow<Boolean> = visualMatchingManager.isCaptureActive
    val isSearching: StateFlow<Boolean> = visualMatchingManager.isRunning

    private val _templates = MutableStateFlow<List<ObjectTemplate>>(emptyList())
    val templates: StateFlow<List<ObjectTemplate>> = _templates.asStateFlow()

    private val _lastResult = MutableStateFlow<MatchResult?>(null)
    val lastResult: StateFlow<MatchResult?> = _lastResult.asStateFlow()

    private val _lastProcessingTimeMillis = MutableStateFlow(0L)
    val lastProcessingTimeMillis: StateFlow<Long> = _lastProcessingTimeMillis.asStateFlow()

    private val _overlayEnabled = MutableStateFlow(false)
    val overlayEnabled: StateFlow<Boolean> = _overlayEnabled.asStateFlow()

    private val _searchedTemplateId = MutableStateFlow<String?>(null)
    val searchedTemplateId: StateFlow<String?> = _searchedTemplateId.asStateFlow()

    init {
        refreshTemplates()
    }

    fun refreshTemplates() {
        viewModelScope.launch { _templates.value = objectRepository.getTemplates() }
    }

    fun createCaptureIntent(): Intent = visualMatchingManager.createCaptureIntent()

    fun onCaptureGranted(resultCode: Int, data: Intent) = visualMatchingManager.startCapture(resultCode, data)

    fun onStopCaptureClicked() {
        visualMatchingManager.stopCapture()
        debugOverlayManager.hide()
        _lastResult.value = null
        _searchedTemplateId.value = null
    }

    fun onFindClicked(templateId: String) {
        _searchedTemplateId.value = templateId
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            val result = visualMatchingManager.findObject(templateId)
            _lastProcessingTimeMillis.value = System.currentTimeMillis() - startTime
            _lastResult.value = result

            if (_overlayEnabled.value) {
                debugOverlayManager.update(
                    result?.let {
                        DebugOverlayInfo(
                            boundingBox = it.boundingBox,
                            confidence = it.confidence,
                            templateId = it.templateId,
                            processingTimeMillis = _lastProcessingTimeMillis.value,
                            method = it.method
                        )
                    }
                )
            }
        }
    }

    fun onOverlayToggled(enabled: Boolean) {
        _overlayEnabled.value = enabled
        if (enabled) debugOverlayManager.show() else debugOverlayManager.hide()
    }

    override fun onCleared() {
        visualMatchingManager.cancel()
        debugOverlayManager.hide()
        super.onCleared()
    }
}
