package com.behaviorengine.core.domain.engine

/**
 * The finite set of lifecycle states the [EngineManager] can be in.
 *
 * Kept as a closed enum (rather than open flags) because the engine lifecycle is a strict
 * state machine: exactly one status holds at any moment, and every future subsystem
 * (vision, recognition, behavior, automation...) will key its own readiness off of this.
 */
enum class EngineStatus {
    OFFLINE,
    STARTING,
    RUNNING,
    PAUSED,
    STOPPING,
    ERROR
}
