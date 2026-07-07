package com.behaviorengine.core.domain.objectlearning

import android.graphics.Bitmap

/** Crops the detected object out of a frame and derives its binary mask. */
interface CropManager {

    /** Crops [frame] to [boundingBox] padded by [PADDING_FRACTION], clamped to the frame's bounds so the crop never fails from clipping. */
    fun cropObject(frame: Bitmap, boundingBox: BoundingBox): Bitmap

    /** Builds a binary (opaque/transparent) mask the same size as [cropped], for a future phase's template matching. */
    fun generateMask(cropped: Bitmap): Bitmap

    companion object {
        const val PADDING_FRACTION = 0.10f
        const val MIN_CROP_DIMENSION_PX = 24
    }
}
