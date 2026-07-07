package com.behaviorengine.engine

import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.DiagnosticsReport
import com.behaviorengine.core.domain.engine.EngineDiagnosticsManager
import com.behaviorengine.core.domain.engine.EngineHealthMonitor
import com.behaviorengine.core.domain.engine.EngineMetrics
import com.behaviorengine.core.domain.engine.EngineObserver
import com.behaviorengine.core.domain.engine.EngineValidator
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [EngineDiagnosticsManager]; see that interface for its role. */
@Singleton
class EngineDiagnosticsManagerImpl @Inject constructor(
    private val engineMetrics: EngineMetrics,
    private val engineHealthMonitor: EngineHealthMonitor,
    private val engineValidator: EngineValidator,
    private val engineObserver: EngineObserver,
    private val loggerManager: LoggerManager
) : EngineDiagnosticsManager {

    override fun runDiagnostics(): DiagnosticsReport {
        val report = DiagnosticsReport(
            metrics = engineMetrics.snapshot(),
            health = engineHealthMonitor.snapshot.value,
            validation = engineValidator.validate(),
            observed = engineObserver.snapshot.value
        )

        if (report.validation.isValid) {
            loggerManager.i(TAG, "Diagnostics OK: ${report.metrics}")
        } else {
            val summary = report.validation.issues.joinToString { "${it.component}: ${it.message}" }
            loggerManager.w(TAG, "Diagnostics found ${report.validation.issues.size} issue(s): $summary")
        }

        return report
    }

    private companion object {
        const val TAG = "EngineDiagnosticsManager"
    }
}
