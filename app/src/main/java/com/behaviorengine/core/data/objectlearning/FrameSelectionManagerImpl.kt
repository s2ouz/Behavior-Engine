package com.behaviorengine.core.data.objectlearning

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.behaviorengine.core.domain.objectlearning.FrameSelectionManager
import com.behaviorengine.core.domain.teaching.ScreenFrame
import com.behaviorengine.core.domain.teaching.TeachingRepository
import com.behaviorengine.core.domain.teaching.TouchSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class FrameSelectionManagerImpl @Inject constructor(
    private val teachingRepository: TeachingRepository
) : FrameSelectionManager {

    override suspend fun findClosestFrame(sessionId: String, touch: TouchSample): ScreenFrame? =
        teachingRepository.getFrames(sessionId)
            .minByOrNull { abs(it.timestampMillis - touch.timestampMillis) }

    override suspend fun loadFrame(frame: ScreenFrame): Bitmap? = withContext(Dispatchers.IO) {
        runCatching { BitmapFactory.decodeFile(frame.imagePath) }.getOrNull()
    }

    override fun validateFrame(frame: ScreenFrame, touch: TouchSample): Boolean =
        abs(frame.timestampMillis - touch.timestampMillis) <= FrameSelectionManager.MAX_FRAME_DIFFERENCE_MILLIS
}
