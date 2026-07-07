package com.behaviorengine.core.domain.teaching

import kotlinx.serialization.Serializable

/**
 * A single teaching recording — device context, timing, and running counters for one
 * screen-capture + touch-collection session. Supersedes v0.8.0's simpler, object-linked
 * `TeachingSession`: this phase's session is no longer tied to a single
 * [com.behaviorengine.core.domain.objects.VisualObject] up front — it just records "what
 * happened on screen," for a future phase (Object Detection) to associate with taught objects.
 *
 * `@Serializable` because [com.behaviorengine.core.domain.teaching.TeachingStorage] writes this
 * straight to `session.json` via kotlinx.serialization — there is no other persistence layer this
 * phase, per the spec's "JSON Database" storage design.
 *
 * @param id Stable identifier, assigned once at creation.
 * @param name Human-readable label, e.g. "Session 1" — not user-editable this phase.
 * @param status See [TeachingState].
 * @param createdAtMillis Wall-clock time the session object was created (before recording begins).
 * @param startedAtMillis Wall-clock time recording actually started, or `null` until it does.
 * @param finishedAtMillis Wall-clock time recording ended (Completed or Cancelled), or `null` while in flight.
 * @param durationMillis Total recording duration; computed once finished, 0 while in flight.
 * @param packageName Best-effort foreground app package at session start. Detecting the true
 * foreground app system-wide needs `PACKAGE_USAGE_STATS` (a separate, user-granted special
 * permission not in this phase's required list) — see [com.behaviorengine.core.data.teaching.SessionManagerImpl]
 * for the graceful fallback when that access hasn't been granted.
 * @param applicationName Best-effort human-readable label for [packageName].
 * @param deviceModel [android.os.Build.MODEL].
 * @param manufacturer [android.os.Build.MANUFACTURER].
 * @param androidVersion [android.os.Build.VERSION.RELEASE].
 * @param screenWidth Pixel width of the device screen at session start.
 * @param screenHeight Pixel height of the device screen at session start.
 * @param density Screen density (`DisplayMetrics.density`) at session start.
 * @param orientation `"portrait"` or `"landscape"` at session start.
 * @param frameCount Running count of [ScreenFrame]s saved so far.
 * @param touchCount Running count of [TouchSample]s saved so far.
 */
@Serializable
data class TeachingSession(
    val id: String,
    val name: String,
    val status: TeachingState,
    val createdAtMillis: Long,
    val startedAtMillis: Long? = null,
    val finishedAtMillis: Long? = null,
    val durationMillis: Long = 0L,
    val packageName: String,
    val applicationName: String,
    val deviceModel: String,
    val manufacturer: String,
    val androidVersion: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val density: Float,
    val orientation: String,
    val frameCount: Int = 0,
    val touchCount: Int = 0
)
