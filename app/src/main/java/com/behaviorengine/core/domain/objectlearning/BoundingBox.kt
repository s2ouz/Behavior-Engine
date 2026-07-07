package com.behaviorengine.core.domain.objectlearning

import kotlinx.serialization.Serializable

/** A pixel rectangle in a captured frame's coordinate space — left/top/right/bottom, not x/y/width/height, so it composes cleanly with clamping against frame bounds. */
@Serializable
data class BoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val area: Int get() = width * height
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f

    /** Grows this box by [fraction] of its own size on every side, without exceeding [maxWidth]/[maxHeight]. */
    fun padded(fraction: Float, maxWidth: Int, maxHeight: Int): BoundingBox {
        val padX = (width * fraction).toInt().coerceAtLeast(1)
        val padY = (height * fraction).toInt().coerceAtLeast(1)
        return BoundingBox(
            left = (left - padX).coerceAtLeast(0),
            top = (top - padY).coerceAtLeast(0),
            right = (right + padX).coerceAtMost(maxWidth),
            bottom = (bottom + padY).coerceAtMost(maxHeight)
        )
    }

    fun distanceTo(x: Float, y: Float): Float {
        val dx = centerX - x
        val dy = centerY - y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
