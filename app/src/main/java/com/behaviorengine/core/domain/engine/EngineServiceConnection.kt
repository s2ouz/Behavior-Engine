package com.behaviorengine.core.domain.engine

import kotlinx.coroutines.flow.StateFlow

/**
 * Framework-free handle onto the engine's durable background host — in practice
 * [com.behaviorengine.services.EngineService], an Android foreground Service — kept out of
 * this signature entirely so the domain layer only has to know "is a durable host connected
 * right now," never that it happens to be an Android Service under the hood.
 *
 * [connect] and [disconnect] must only ever be called by
 * [com.behaviorengine.engine.EngineManagerImpl]: see [EngineManager]'s docs for why nothing
 * else — not a ViewModel, not the Service itself — is allowed to control the host directly.
 */
interface EngineServiceConnection {

    /** Whether the background host is currently up. */
    val isConnected: StateFlow<Boolean>

    /** Starts the background host if it isn't already running. Idempotent. */
    fun connect()

    /** Stops the background host if it's running. Idempotent. */
    fun disconnect()
}
