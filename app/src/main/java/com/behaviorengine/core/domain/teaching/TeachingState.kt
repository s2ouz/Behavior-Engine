package com.behaviorengine.core.domain.teaching

/**
 * Lifecycle states of a [TeachingSession]. Supersedes v0.8.0's `TeachingStatus` — this phase's
 * teaching session is a real, ongoing recording (screen frames + touches), so its state machine
 * needs `Preparing` (permissions/projection being set up) and `Stopping` (tearing down capture)
 * as distinct steps, not just instantaneous transitions.
 */
enum class TeachingState {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    STOPPING,
    COMPLETED,
    CANCELLED
}
