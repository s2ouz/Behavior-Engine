package com.behaviorengine.core.domain.engine

/**
 * Determines initialization order in [ModuleRegistry.getAllModules] (and thus start order):
 * lower ordinal runs first. A future Vision module would register CRITICAL so it's ready
 * before a HIGH-priority Recognition module that depends on it, without either module needing
 * to know about the other directly.
 */
enum class ModulePriority {
    CRITICAL,
    HIGH,
    NORMAL,
    LOW,
    BACKGROUND
}
