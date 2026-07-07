package com.behaviorengine.core.domain.objectlearning

import android.graphics.Bitmap

/** Measures [VisualFeatures] from a cropped object image and its mask. */
interface FeatureExtractionManager {

    fun extractFeatures(cropped: Bitmap, mask: Bitmap, boundingBox: BoundingBox): VisualFeatures
}
