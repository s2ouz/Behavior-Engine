package com.behaviorengine.core.domain.objectlearning

import android.graphics.Bitmap
import com.behaviorengine.core.domain.teaching.ScreenFrame
import com.behaviorengine.core.domain.teaching.TouchSample

/** Finds and loads the [ScreenFrame] closest in time to a given [TouchSample]. */
interface FrameSelectionManager {

    /** The frame with the smallest `|timestampMillis|` difference to [touch], or `null` if the session has no frames. */
    suspend fun findClosestFrame(sessionId: String, touch: TouchSample): ScreenFrame?

    /** Decodes [frame]'s image file from disk, or `null` if it's missing/corrupted. */
    suspend fun loadFrame(frame: ScreenFrame): Bitmap?

    /** True only if [frame] is within [MAX_FRAME_DIFFERENCE_MILLIS] of [touch]. */
    fun validateFrame(frame: ScreenFrame, touch: TouchSample): Boolean

    companion object {
        /** Maximum allowed frame/touch time difference per spec. */
        const val MAX_FRAME_DIFFERENCE_MILLIS = 100L
    }
}
