package com.behaviorengine.core.domain.teaching

/**
 * The glue between capture and storage: subscribes to [ScreenCaptureManager.frames] and
 * [TouchCollectorManager.touches] for the running session, stamps each with a frame/sequence
 * number, and writes them through [TeachingRepository] — "Receive screen frames, receive touch
 * events, synchronize timestamps, write data to repository," per spec. Nothing here inspects what
 * a frame or touch *means*; it only persists what already happened.
 */
interface TeachingRecorder {

    /** Begins forwarding frames/touches for [session] until [stop]. */
    fun start(session: TeachingSession)

    /** Stops forwarding; safe to call whether or not [start] was ever called. */
    fun stop()
}
