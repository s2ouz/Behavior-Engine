package com.behaviorengine

import android.app.Application
import com.behaviorengine.core.common.AppConstants
import com.behaviorengine.core.common.LoggerManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point and Hilt component root. Every `@Inject`-annotated class in the app
 * traces back to the [SingletonComponent][dagger.hilt.components.SingletonComponent] generated
 * from this class.
 */
@HiltAndroidApp
class BehaviorEngineApp : Application() {

    @Inject
    lateinit var loggerManager: LoggerManager

    override fun onCreate() {
        super.onCreate()
        loggerManager.init(AppConstants.DEBUG_MODE)
        loggerManager.i(TAG, "${AppConstants.PROJECT_NAME} ${AppConstants.APP_VERSION} starting")
    }

    private companion object {
        const val TAG = "BehaviorEngineApp"
    }
}
