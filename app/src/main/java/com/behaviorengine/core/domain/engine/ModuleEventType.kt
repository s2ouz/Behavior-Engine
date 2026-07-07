package com.behaviorengine.core.domain.engine

/** What happened to a module, carried by [EngineEvent.ModuleEvent]; see [ModuleRegistry]. */
enum class ModuleEventType {
    REGISTERED,
    REMOVED,
    ENABLED,
    DISABLED
}
