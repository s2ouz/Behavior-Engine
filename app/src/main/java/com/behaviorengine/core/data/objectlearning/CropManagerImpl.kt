package com.behaviorengine.core.data.objectlearning

import android.graphics.Bitmap
import android.graphics.Color
import com.behaviorengine.core.domain.objectlearning.BoundingBox
import com.behaviorengine.core.domain.objectlearning.CropManager
import javax.inject.Inject
import javax.inject.Singleton

private const val BACKGROUND_DISTANCE_THRESHOLD = 40.0

/**
 * Real implementation of [CropManager]. [generateMask] is a background-subtraction heuristic, not
 * a trained segmentation model: it estimates the background color from the crop's outer border
 * pixels (UI elements are usually not flush against their own crop edges thanks to the 10%
 * padding), then marks every pixel far enough from that estimate as "object." Cheap, real, and
 * good enough for the mask's stated purpose ("future template matching") without a model this
 * project has no way to train or bundle.
 */
@Singleton
class CropManagerImpl @Inject constructor() : CropManager {

    override fun cropObject(frame: Bitmap, boundingBox: BoundingBox): Bitmap {
        val padded = boundingBox.padded(CropManager.PADDING_FRACTION, frame.width, frame.height)
        val width = padded.width.coerceAtLeast(1).coerceAtMost(frame.width - padded.left)
        val height = padded.height.coerceAtLeast(1).coerceAtMost(frame.height - padded.top)
        return Bitmap.createBitmap(frame, padded.left, padded.top, width, height)
    }

    override fun generateMask(cropped: Bitmap): Bitmap {
        val width = cropped.width
        val height = cropped.height
        val backgroundColor = estimateBorderColor(cropped)

        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        cropped.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val distance = colorDistance(pixels[i], backgroundColor)
            pixels[i] = if (distance > BACKGROUND_DISTANCE_THRESHOLD) Color.WHITE else Color.TRANSPARENT
        }
        mask.setPixels(pixels, 0, width, 0, 0, width, height)
        return mask
    }

    private fun estimateBorderColor(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0
        val samples = sequence {
            for (x in 0 until width) {
                yield(bitmap.getPixel(x, 0))
                yield(bitmap.getPixel(x, height - 1))
            }
            for (y in 0 until height) {
                yield(bitmap.getPixel(0, y))
                yield(bitmap.getPixel(width - 1, y))
            }
        }
        for (pixel in samples) {
            r += Color.red(pixel)
            g += Color.green(pixel)
            b += Color.blue(pixel)
            count++
        }
        if (count == 0) return Color.BLACK
        return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun colorDistance(a: Int, b: Int): Double {
        val dr = (Color.red(a) - Color.red(b)).toDouble()
        val dg = (Color.green(a) - Color.green(b)).toDouble()
        val db = (Color.blue(a) - Color.blue(b)).toDouble()
        return kotlin.math.sqrt(dr * dr + dg * dg + db * db)
    }
}
