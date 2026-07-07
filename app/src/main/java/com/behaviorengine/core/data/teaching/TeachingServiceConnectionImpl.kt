package com.behaviorengine.core.data.teaching

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.teaching.TeachingServiceConnection
import com.behaviorengine.services.TeachingOverlayService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [TeachingServiceConnection]; see that interface for the ownership rules. */
@Singleton
class TeachingServiceConnectionImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggerManager: LoggerManager
) : TeachingServiceConnection {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun connect(sessionId: String, resultCode: Int, data: Intent) {
        if (_isConnected.value) return
        ContextCompat.startForegroundService(context, TeachingOverlayService.startIntent(context, sessionId, resultCode, data))
        _isConnected.value = true
        loggerManager.i(TAG, "TeachingOverlayService connect requested")
    }

    override fun disconnect() {
        if (!_isConnected.value) return
        context.startService(TeachingOverlayService.stopIntent(context))
        _isConnected.value = false
        loggerManager.i(TAG, "TeachingOverlayService disconnect requested")
    }

    private companion object {
        const val TAG = "TeachingServiceConnection"
    }
}
