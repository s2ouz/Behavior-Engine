package com.behaviorengine.engine

import com.behaviorengine.core.domain.engine.EngineManager
import com.behaviorengine.core.domain.engine.EngineServiceConnection
import com.behaviorengine.core.domain.engine.RuntimeController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [EngineManager]. Deliberately thin: every method is a one- or
 * two-line delegation, because the actual work now belongs to [RuntimeController] (tick-loop
 * mechanics) and [EngineServiceConnection] (the background host). This class exists solely to
 * couple those two orthogonal concerns at exactly the right moments — connecting the host only
 * once [initialize] actually succeeds, disconnecting it only once [reset] actually succeeds —
 * without either of them needing to know the other exists.
 */
@Singleton
class EngineManagerImpl @Inject constructor(
    private val runtimeController: RuntimeController,
    private val serviceConnection: EngineServiceConnection
) : EngineManager {

    override fun initialize() {
        if (runtimeController.initialize()) {
            serviceConnection.connect()
        }
    }

    override fun start() {
        runtimeController.start()
    }

    override fun pause() {
        runtimeController.pause()
    }

    override fun resume() {
        runtimeController.resume()
    }

    override fun stop() {
        runtimeController.stop()
    }

    override fun reset() {
        if (runtimeController.reset()) {
            serviceConnection.disconnect()
        }
    }
}
