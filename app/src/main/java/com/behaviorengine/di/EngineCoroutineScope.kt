package com.behaviorengine.di

import javax.inject.Qualifier

/**
 * Qualifies the single process-lifetime [kotlinx.coroutines.CoroutineScope] shared by every
 * engine singleton that needs one ([com.behaviorengine.engine.EngineLoopImpl],
 * [com.behaviorengine.engine.EngineObserverImpl], [com.behaviorengine.engine.EngineStateStoreImpl],
 * [com.behaviorengine.engine.EngineHealthMonitorImpl]). Before this, each of those four
 * constructed its own identical `CoroutineScope(SupervisorJob() + Dispatchers.Default)` — same
 * duplicated code four times, and four separate scopes to reason about instead of one.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EngineCoroutineScope
