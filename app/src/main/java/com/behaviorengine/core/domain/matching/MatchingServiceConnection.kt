package com.behaviorengine.core.domain.matching

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow

/**
 * Framework-free handle onto [com.behaviorengine.services.VisualMatchingService], mirroring
 * [com.behaviorengine.core.domain.teaching.TeachingServiceConnection]'s role for Teaching Mode.
 * Only [com.behaviorengine.core.data.matching.VisualMatchingManagerImpl] should call
 * [connect]/[disconnect].
 */
interface MatchingServiceConnection {
    val isConnected: StateFlow<Boolean>

    /** Starts the foreground capture host, handing off the granted MediaProjection consent result. */
    fun connect(resultCode: Int, data: Intent)

    /** Stops the host. Idempotent. */
    fun disconnect()
}
