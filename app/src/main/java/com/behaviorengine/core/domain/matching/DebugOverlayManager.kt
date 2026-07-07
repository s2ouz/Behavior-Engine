package com.behaviorengine.core.domain.matching

import com.behaviorengine.core.domain.objectlearning.BoundingBox

data class DebugOverlayInfo(
    val boundingBox: BoundingBox,
    val confidence: Int,
    val templateId: String,
    val processingTimeMillis: Long,
    val method: String
)

/**
 * Development-builds-only overlay drawing a [MatchResult]'s bounding box/confidence directly on
 * screen, per spec. A plain [android.view.View]/[android.graphics.Canvas] window added via
 * `WindowManager`, not a `ComposeView` — same reasoning as
 * [com.behaviorengine.core.domain.teaching.OverlayManager]'s KDoc: a `ComposeView` hosted outside
 * any `Activity` needs a hand-rolled `LifecycleOwner` trio to avoid crashing, more risk than this
 * window (one rectangle, one text label) justifies.
 */
interface DebugOverlayManager {
    fun show()
    fun hide()
    fun update(info: DebugOverlayInfo?)
}
