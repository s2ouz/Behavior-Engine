package com.behaviorengine.engine

import android.content.Context
import androidx.core.content.ContextCompat
import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.EngineServiceConnection
import com.behaviorengine.services.EngineService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [EngineServiceConnection]; see that interface for the ownership rules. */
@Singleton
class EngineServiceConnectionImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val loggerManager: LoggerManager
) : EngineServiceConnection {

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override fun connect() {
        if (_isConnected.value) return
        ContextCompat.startForegroundService(context, EngineService.intent(context))
        _isConnected.value = true
        loggerManager.i(TAG, "EngineService connect requested")
    }

    override fun disconnect() {
        if (!_isConnected.value) return
        context.stopService(EngineService.intent(context))
        _isConnected.value = false
        loggerManager.i(TAG, "EngineService disconnect requested")
    }

    private companion object {
        const val TAG = "EngineServiceConnection"
    }
}
