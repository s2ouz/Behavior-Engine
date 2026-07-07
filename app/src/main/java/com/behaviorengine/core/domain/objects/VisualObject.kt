package com.behaviorengine.core.domain.objects

import androidx.compose.runtime.Immutable

/**
 * A single taught visual object — the unit everything in this product will eventually operate
 * on (recognition, automation, feedback). No image data lives here yet; this phase only manages
 * the object's identity and metadata, per its "no image processing" scope.
 *
 * `@Immutable`: Compose's stability inference would otherwise flag this class unstable purely
 * because [reserved] is a `Map`, forcing unnecessary recomposition of every card in
 * `ObjectsScreen`'s `LazyColumn` on any unrelated state change. The annotation is honest here —
 * every mutation goes through [com.behaviorengine.core.domain.objects.VisualObjectRepository],
 * which always publishes a new instance via `copy()`, never mutates one in place.
 *
 * @param id Stable identifier, assigned once at creation.
 * @param name Display name; the only field a user can currently influence (there is no editing
 * UI yet, so today this is only set once, at creation).
 * @param createdAtMillis Wall-clock time this object was created.
 * @param lastModifiedMillis Wall-clock time of the most recent [VisualObjectRepository.updateObject] call.
 * @param status See [VisualObjectStatus].
 * @param imageCount How many training images back this object; always 0 until teaching captures any.
 * @param recognitionEnabled Whether this object participates in (future) recognition passes.
 * @param notes Free-text user notes; not yet surfaced in any screen, prepared for a future editing UI.
 * @param reserved Free-form slot for future AI metadata (embeddings, confidence thresholds, model
 * version) without reshaping this data class — the same pattern used by
 * [com.behaviorengine.core.domain.engine.EngineState.reserved] and
 * [com.behaviorengine.core.domain.profile.UserProfile.reserved].
 */
@Immutable
data class VisualObject(
    val id: String,
    val name: String,
    val createdAtMillis: Long,
    val lastModifiedMillis: Long,
    val status: VisualObjectStatus = VisualObjectStatus.READY,
    val imageCount: Int = 0,
    val recognitionEnabled: Boolean = false,
    val notes: String = "",
    val reserved: Map<String, String> = emptyMap()
)
