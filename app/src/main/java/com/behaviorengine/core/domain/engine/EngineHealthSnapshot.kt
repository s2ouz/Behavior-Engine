package com.behaviorengine.core.domain.engine

/**
 * Read-only vitals maintained by [EngineHealthMonitor]. "Only reporting" per this phase's
 * spec — nothing reacts to a bad snapshot yet; a future phase deciding whether to, say, back
 * off automation after repeated errors would read from here rather than re-deriving these
 * checks itself.
 *
 * @param engineAlive True once the engine has left OFFLINE (an active session exists).
 * @param runtimeActive True only while the tick loop is actually RUNNING.
 * @param serviceConnected Mirrors [EngineServiceConnection.isConnected].
 * @param lifecycleValid False only while stuck in [EngineStatus.ERROR].
 * @param moduleCount Total modules known to [ModuleRegistry], active or not.
 * @param errorCount Mirrors [EngineObserverSnapshot.errorCount].
 * @param warningCount Mirrors [EngineObserverSnapshot.warningCount].
 */
data class EngineHealthSnapshot(
    val engineAlive: Boolean = false,
    val runtimeActive: Boolean = false,
    val serviceConnected: Boolean = false,
    val lifecycleValid: Boolean = true,
    val moduleCount: Int = 0,
    val errorCount: Int = 0,
    val warningCount: Int = 0
)
