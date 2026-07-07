package com.behaviorengine.engine

import com.behaviorengine.core.common.ConfigManager
import com.behaviorengine.core.domain.engine.EngineHealthMonitor
import com.behaviorengine.core.domain.engine.EngineValidator
import com.behaviorengine.core.domain.engine.ModuleRegistry
import com.behaviorengine.core.domain.engine.ValidationIssue
import com.behaviorengine.core.domain.engine.ValidationReport
import javax.inject.Inject
import javax.inject.Singleton

/** Real implementation of [EngineValidator]; see that interface for what these checks are for. */
@Singleton
class EngineValidatorImpl @Inject constructor(
    private val moduleRegistry: ModuleRegistry,
    private val configManager: ConfigManager,
    private val engineHealthMonitor: EngineHealthMonitor
) : EngineValidator {

    override fun validate(): ValidationReport {
        val issues = mutableListOf<ValidationIssue>()

        checkModuleRegistry(issues)
        checkEngineConfig(issues)
        checkHealthCoherence(issues)

        return ValidationReport(issues = issues, checkedAtMillis = System.currentTimeMillis())
    }

    private fun checkModuleRegistry(issues: MutableList<ValidationIssue>) {
        val allModules = moduleRegistry.getAllModules()
        val allIds = allModules.map { it.id }
        if (allIds.distinct().size != allIds.size) {
            issues += ValidationIssue(COMPONENT_MODULE_REGISTRY, "Duplicate module ids among registered modules.")
        }

        val activeIds = moduleRegistry.getActiveModules().map { it.id }
        if (!allIds.toSet().containsAll(activeIds)) {
            issues += ValidationIssue(COMPONENT_MODULE_REGISTRY, "An active module is missing from the full module list.")
        }
    }

    private fun checkEngineConfig(issues: MutableList<ValidationIssue>) {
        val tickRate = configManager.engineConfig.value.targetTickRate
        if (tickRate.intervalMillis <= 0L) {
            issues += ValidationIssue(
                COMPONENT_ENGINE_CONFIG,
                "Configured tick rate '${tickRate.name}' resolves to a non-positive interval."
            )
        }
    }

    private fun checkHealthCoherence(issues: MutableList<ValidationIssue>) {
        val health = engineHealthMonitor.snapshot.value
        if (health.runtimeActive && !health.engineAlive) {
            issues += ValidationIssue(
                COMPONENT_HEALTH_MONITOR,
                "Runtime reported active while the engine is not considered alive."
            )
        }
        if (health.runtimeActive && !health.lifecycleValid) {
            issues += ValidationIssue(
                COMPONENT_HEALTH_MONITOR,
                "Runtime reported active while the lifecycle is in an invalid (ERROR) state."
            )
        }
    }

    private companion object {
        const val COMPONENT_MODULE_REGISTRY = "ModuleRegistry"
        const val COMPONENT_ENGINE_CONFIG = "EngineConfig"
        const val COMPONENT_HEALTH_MONITOR = "EngineHealthMonitor"
    }
}
