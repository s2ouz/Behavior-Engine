package com.behaviorengine.core.domain.teaching

import kotlinx.coroutines.flow.StateFlow

/** What the floating overlay bubble displays — refreshed via [OverlayManager.update]. */
data class OverlayStats(
    val state: TeachingState,
    val recordingTimeMillis: Long,
    val frameCount: Int,
    val touchCount: Int
)

/** User taps on the overlay's own controls, wired by whoever calls [OverlayManager.show]. */
data class OverlayCallbacks(
    val onPauseClick: () -> Unit,
    val onResumeClick: () -> Unit,
    val onStopClick: () -> Unit,
    val onCancelClick: () -> Unit
)

/**
 * Controls the floating teaching overlay window — draggable, minimal, always on top while
 * recording. Needs `SYSTEM_ALERT_WINDOW`; see [com.behaviorengine.core.data.teaching.OverlayManagerImpl]
 * for the `WindowManager` details. Deliberately built with plain Android views rather than a
 * `ComposeView`: a `ComposeView` hosted outside any `Activity` needs a hand-rolled
 * `LifecycleOwner`/`ViewModelStoreOwner`/`SavedStateRegistryOwner` trio to avoid crashing, which is
 * meaningfully more risk for a window this simple (a status readout and four buttons).
 */
interface OverlayManager {

    val isShowing: StateFlow<Boolean>

    val isLocked: StateFlow<Boolean>

    /** Adds the overlay window, wiring its buttons to [callbacks]. No-op if already showing. */
    fun show(callbacks: OverlayCallbacks)

    /** Removes the overlay window. No-op if not showing. */
    fun hide()

    /** Refreshes the on-screen REC indicator / timer / counts from [stats]. */
    fun update(stats: OverlayStats)

    /** Repositions the overlay window to [x]/[y] (screen coordinates of its top-left corner). */
    fun move(x: Int, y: Int)

    /** Disables dragging — the bubble stays where it is until [unlock]. */
    fun lock()

    fun unlock()
}
