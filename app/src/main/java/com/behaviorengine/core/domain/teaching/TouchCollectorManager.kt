package com.behaviorengine.core.domain.teaching

import android.view.MotionEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Collects touch events into [TouchSample]s while a session is recording. Scope note: this phase
 * only observes touches delivered to the teaching overlay's own views (the draggable bubble and
 * its buttons) — see [TouchSample]'s KDoc for why true system-wide touch capture is out of scope
 * this phase, and how a future phase plugs into [recordTouch] without changing this contract.
 */
interface TouchCollectorManager {

    /** Emits one [TouchSample] per recorded touch while collecting. */
    val touches: SharedFlow<TouchSample>

    fun startCollecting(sessionId: String)

    fun stopCollecting()

    /** Builds a [TouchSample] from [event] (stamped with the session passed to [startCollecting]) and emits it. */
    fun recordTouch(event: MotionEvent)

    /** Drops any buffered state without emitting. Safe to call whether or not currently collecting. */
    fun clear()
}
