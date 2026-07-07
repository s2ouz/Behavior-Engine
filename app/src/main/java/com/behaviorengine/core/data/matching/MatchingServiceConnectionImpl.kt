package com.behaviorengine.core.data.matching

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.matching.MatchingServiceConnection
import com.behaviorengine.services.VisualMatchingService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [MatchingServiceConnection]; see that interface for the ownership rules. */
@Singleton
class MatchingServiceConnectionImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggerManager: LoggerManager
) : MatchingServiceConnection {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun connect(resultCode: Int, data: Intent) {
        if (_isConnected.value) return
        ContextCompat.startForegroundService(context, VisualMatchingService.startIntent(context, resultCode, data))
        _isConnected.value = true
        loggerManager.i(TAG, "VisualMatchingService connect requested")
    }

    override fun disconnect() {
        if (!_isConnected.value) return
        context.startService(VisualMatchingService.stopIntent(context))
        _isConnected.value = false
        loggerManager.i(TAG, "VisualMatchingService disconnect requested")
    }

    private companion object {
        const val TAG = "MatchingServiceConnection"
    }
}
