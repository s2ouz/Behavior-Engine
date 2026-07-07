package com.behaviorengine.core.domain.teaching

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Framework-free handle onto [com.behaviorengine.services.TeachingOverlayService], mirroring
 * [com.behaviorengine.core.domain.engine.EngineServiceConnection]'s role for the engine. Only
 * [com.behaviorengine.core.data.teaching.TeachingModeManagerImpl] should call [connect]/[disconnect].
 */
interface TeachingServiceConnection {

    val isConnected: StateFlow<Boolean>

    /** Starts the foreground overlay/capture host for [sessionId], handing off the granted MediaProjection consent result. */
    fun connect(sessionId: String, resultCode: Int, data: Intent)

    /** Stops the host. Idempotent. */
    fun disconnect()
}
