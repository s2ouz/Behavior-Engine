package com.behaviorengine.core.domain.teaching

import kotlinx.serialization.Serializable

/**
 * A single collected touch event. Scope note: [TouchCollectorManager] this phase only observes
 * touches on the teaching overlay's own views (the draggable bubble and its controls) — capturing
 * raw touch coordinates system-wide, across whatever app the user is teaching on, needs either
 * root, a system-signature permission, or (on API 34+) an `AccessibilityService` declaring
 * `FLAG_REQUEST_MOTION_EVENTS`, none of which are in this phase's permission list. Every touch
 * this phase legitimately observes still carries real [android.view.MotionEvent] data (pressure,
 * size, pointer count) — nothing here is faked — so a future phase can add system-wide capture by
 * feeding more samples through the same [TouchCollectorManager.recordTouch] without changing this
 * model or any consumer of it.
 *
 * @param action One of `"DOWN"`, `"MOVE"`, `"UP"`, `"CANCEL"` — [android.view.MotionEvent.getAction] in
 * human-readable form, since this is written straight to JSON for a future phase to read.
 */
@Serializable
data class TouchSample(
    val id: String,
    val sessionId: String,
    val x: Float,
    val y: Float,
    val timestampMillis: Long,
    val pressure: Float,
    val size: Float,
    val pointerCount: Int,
    val action: String,
    val orientation: String,
    val screenWidth: Int,
    val screenHeight: Int
)
