package com.behaviorengine.core.data.matching

import android.graphics.Color
import kotlin.math.sqrt

/** Small pixel-math helpers shared across the matching pipeline's stages — kept package-private rather than exposed as public API. */
internal const val MAX_RGB_DISTANCE = 441.7 // sqrt(255^2 * 3): the largest possible distance between two RGB colors

/** 1 minus normalized Hamming distance between two 64-bit dHash hex strings — 1.0 is identical, 0.0 is maximally different. */
internal fun hammingSimilarity(hashHexA: String, hashHexB: String): Float {
    val a = hashHexA.toULongOrNull(16)?.toLong() ?: return 0f
    val b = hashHexB.toULongOrNull(16)?.toLong() ?: return 0f
    val distance = java.lang.Long.bitCount(a xor b)
    return (1f - distance / 64f).coerceIn(0f, 1f)
}

internal fun rgbDistance(a: Int, b: Int): Double {
    val dr = (Color.red(a) - Color.red(b)).toDouble()
    val dg = (Color.green(a) - Color.green(b)).toDouble()
    val db = (Color.blue(a) - Color.blue(b)).toDouble()
    return sqrt(dr * dr + dg * dg + db * db)
}

/** 1 minus [colorA]'s distance to the closest color in [palette], normalized by [MAX_RGB_DISTANCE]. `0f` if [palette] is empty. */
internal fun closestColorSimilarity(colorA: Int, palette: List<Int>): Float {
    if (palette.isEmpty()) return 0f
    val closest = palette.minOf { rgbDistance(colorA, it) }
    return (1.0 - closest / MAX_RGB_DISTANCE).coerceIn(0.0, 1.0).toFloat()
}
