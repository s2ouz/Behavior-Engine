package com.behaviorengine.di

import javax.inject.Qualifier

/**
 * Qualifies a process-lifetime [kotlinx.coroutines.CoroutineScope] for general, non-engine
 * singletons (starting with [com.behaviorengine.core.data.profile.UserProfileRepositoryImpl]).
 * Deliberately separate from [EngineCoroutineScope]: that one is reserved for the engine's own
 * subsystems so a reader always knows "this scope belongs to the engine" from the qualifier
 * alone, rather than one generically-named scope being shared by unrelated feature areas.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
