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

/**
 * Whether tapping "Initialize" is meaningful from this status. Kept as a pure function on
 * [EngineStatus] rather than duplicated as an inline comparison in both
 * [com.behaviorengine.engine.RuntimeControllerImpl] (the enforcement) and
 * `EngineScreen` (the button's `enabled` state) — one definition, two call sites.
 */
fun EngineStatus.canInitialize(): Boolean = this == EngineStatus.OFFLINE

/** Whether tapping "Start" is meaningful from this status; see [canInitialize] for why this exists. */
fun EngineStatus.canStart(): Boolean = this == EngineStatus.READY

/** Whether tapping "Pause" is meaningful from this status; see [canInitialize] for why this exists. */
fun EngineStatus.canPause(): Boolean = this == EngineStatus.RUNNING

/** Whether tapping "Resume" is meaningful from this status; see [canInitialize] for why this exists. */
fun EngineStatus.canResume(): Boolean = this == EngineStatus.PAUSED

/** Whether tapping "Stop" is meaningful from this status; see [canInitialize] for why this exists. */
fun EngineStatus.canStop(): Boolean = this == EngineStatus.RUNNING || this == EngineStatus.PAUSED

/** Whether tapping "Reset" is meaningful from this status; see [canInitialize] for why this exists. */
fun EngineStatus.canReset(): Boolean = this == EngineStatus.STOPPED || this == EngineStatus.ERROR
