package com.behaviorengine.core.domain.teaching

import androidx.compose.runtime.Immutable

/**
 * A single teaching session — the lifecycle wrapper around "the user is about to teach one
 * [com.behaviorengine.core.domain.objects.VisualObject]." Purely a lifecycle record this phase:
 * no image capture happens, so [capturedSamples] stays 0 until a future phase wires in the
 * capture engine.
 *
 * `@Immutable` for the same reason as
 * [com.behaviorengine.core.domain.objects.VisualObject]: [reserved] is a `Map`, and every mutation
 * goes through [TeachingManager], which always publishes a new instance via `copy()`.
 *
 * @param sessionId Stable identifier, assigned once at creation.
 * @param objectId The [com.behaviorengine.core.domain.objects.VisualObject.id] this session teaches.
 * @param createdAtMillis Wall-clock time this session was created.
 * @param status See [TeachingStatus].
 * @param capturedSamples How many training samples this session has captured; always 0 this phase.
 * @param reserved Free-form slot for future capture/AI metadata, the same pattern used by
 * [com.behaviorengine.core.domain.objects.VisualObject.reserved].
 */
@Immutable
data class TeachingSession(
    val sessionId: String,
    val objectId: String,
    val createdAtMillis: Long,
    val status: TeachingStatus,
    val capturedSamples: Int = 0,
    val reserved: Map<String, String> = emptyMap()
)
