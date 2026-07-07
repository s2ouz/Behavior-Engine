package com.behaviorengine.core.domain.engine

/**
 * The finite set of lifecycle states the engine can be in.
 *
 * Modeled after a game engine's boot sequence rather than a simple on/off switch: separate
 * "in transit" states (INITIALIZING, STARTING, PAUSING, RESUMING, STOPPING) exist alongside
 * their settled counterparts (READY, RUNNING, PAUSED, STOPPED) so [EngineLifecycleManager] can
 * validate that, e.g., a caller can't jump straight from RUNNING to INITIALIZING — every
 * transition has to pass through the states in between, exactly like initializing subsystems
 * takes real (if currently instantaneous) time.
 */
enum class EngineStatus {
    OFFLINE,
    INITIALIZING,
    READY,
    STARTING,
    RUNNING,
    PAUSING,
    PAUSED,
    RESUMING,
    STOPPING,
    STOPPED,
    ERROR
}
