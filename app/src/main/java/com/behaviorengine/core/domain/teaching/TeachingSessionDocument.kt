package com.behaviorengine.core.domain.teaching

import kotlinx.serialization.Serializable

/** The root JSON shape [TeachingStorage] writes to `session.json` for one session — see its KDoc. */
@Serializable
data class TeachingSessionDocument(
    val session: TeachingSession,
    val touches: List<TouchSample>,
    val frames: List<ScreenFrame>
)
