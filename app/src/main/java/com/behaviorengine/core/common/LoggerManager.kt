package com.behaviorengine.core.common

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central logging facade. Nothing in the rest of the app calls [Timber] directly; everything
 * goes through this class instead, so the moment a future phase needs to add a crash-reporting
 * tree, a remote-log tree, or to strip logs from release builds entirely, there is exactly one
 * place to change it.
 *
 * Deliberately a plain injectable class rather than a Kotlin `object` singleton: this project
 * standardizes on Hilt-provided dependencies (see [com.behaviorengine.di.AppModule]) so every
 * manager, including this one, can be swapped for a fake in tests.
 */
@Singleton
class LoggerManager @Inject constructor() {

    private var isInitialized = false

    /** Plants the debug logging tree. Safe to call multiple times; only plants once. */
    fun init(debugMode: Boolean) {
        if (isInitialized) return
        if (debugMode) {
            Timber.plant(Timber.DebugTree())
        }
        isInitialized = true
    }

    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Timber.tag(tag).e(throwable, message)
    }
}
