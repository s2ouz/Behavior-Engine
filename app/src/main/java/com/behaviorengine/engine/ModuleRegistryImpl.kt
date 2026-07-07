package com.behaviorengine.engine

import com.behaviorengine.core.common.LoggerManager
import com.behaviorengine.core.domain.engine.EngineEvent
import com.behaviorengine.core.domain.engine.EngineModule
import com.behaviorengine.core.domain.engine.EventBus
import com.behaviorengine.core.domain.engine.ModuleEventType
import com.behaviorengine.core.domain.engine.ModuleRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real implementation of [ModuleRegistry]. All mutation and iteration of [entries] is
 * synchronized on [lock]: [RuntimeControllerImpl] reads [getActiveModules]/[getAllModules] from
 * the tick loop while a future module could in principle register/enable/disable itself from a
 * different thread, so the map itself needs a single consistent view rather than relying on
 * every caller happening to be on the same dispatcher.
 */
@Singleton
class ModuleRegistryImpl @Inject constructor(
    private val eventBus: EventBus,
    private val loggerManager: LoggerManager
) : ModuleRegistry {

    private data class Entry(val module: EngineModule, val enabled: Boolean)

    private val lock = Any()
    private val entries = mutableMapOf<String, Entry>()

    override fun register(module: EngineModule) {
        synchronized(lock) { entries[module.id] = Entry(module, enabled = true) }
        loggerManager.i(TAG, "Registered module '${module.id}' (priority=${module.priority})")
        eventBus.publish(EngineEvent.ModuleEvent(module.id, ModuleEventType.REGISTERED))
    }

    override fun remove(moduleId: String) {
        val removed = synchronized(lock) { entries.remove(moduleId) } != null
        if (removed) eventBus.publish(EngineEvent.ModuleEvent(moduleId, ModuleEventType.REMOVED))
    }

    override fun enable(moduleId: String) {
        val changed = setEnabled(moduleId, enabled = true)
        if (changed) eventBus.publish(EngineEvent.ModuleEvent(moduleId, ModuleEventType.ENABLED))
    }

    override fun disable(moduleId: String) {
        val changed = setEnabled(moduleId, enabled = false)
        if (changed) eventBus.publish(EngineEvent.ModuleEvent(moduleId, ModuleEventType.DISABLED))
    }

    override fun find(moduleId: String): EngineModule? = synchronized(lock) { entries[moduleId]?.module }

    override fun getActiveModules(): List<EngineModule> = synchronized(lock) {
        entries.values.filter { it.enabled }.map { it.module }
    }.sortedBy { it.priority.ordinal }

    override fun getAllModules(): List<EngineModule> = synchronized(lock) {
        entries.values.map { it.module }
    }.sortedBy { it.priority.ordinal }

    private fun setEnabled(moduleId: String, enabled: Boolean): Boolean = synchronized(lock) {
        val entry = entries[moduleId] ?: return@synchronized false
        entries[moduleId] = entry.copy(enabled = enabled)
        true
    }

    private companion object {
        const val TAG = "ModuleRegistry"
    }
}
