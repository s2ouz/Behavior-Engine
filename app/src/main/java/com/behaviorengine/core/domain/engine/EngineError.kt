package com.behaviorengine.core.domain.engine

/**
 * Everything that can go wrong inside the engine, typed instead of thrown-and-caught-generically.
 * [RuntimeController] catches exceptions from module lifecycle calls and wraps them into one of
 * these before forcing [EngineStatus.ERROR] via [EngineLifecycleManager.forceError], so every
 * failure the UI or logs ever see has a known shape instead of an arbitrary [Throwable].
 */
sealed class EngineError(open val message: String) {

    /** Something failed while a module (or the engine itself) was initializing. */
    data class InitializationError(override val message: String) : EngineError(message)

    /** A specific module threw during one of its lifecycle calls; [moduleId] identifies which. */
    data class ModuleError(val moduleId: String, override val message: String) : EngineError(message)

    /** An [EngineConfig] value was invalid or unusable (e.g. an unsupported tick rate). */
    data class ConfigurationError(override val message: String) : EngineError(message)

    /** A failure while the engine was actively running, not attributable to a single module. */
    data class RuntimeError(override val message: String) : EngineError(message)

    /** Fallback for failures that don't fit the categories above. */
    data class UnknownError(override val message: String) : EngineError(message)
}
