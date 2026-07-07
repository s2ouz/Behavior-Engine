package com.behaviorengine.core.domain.teaching

/** Lifecycle states of a [TeachingSession]. No state here implies capture happened — that's a future phase. */
enum class TeachingStatus {
    CREATED,
    PREPARING,
    READY,
    RUNNING,
    PAUSED,
    STOPPED,
    FINISHED,
    CANCELLED
}
