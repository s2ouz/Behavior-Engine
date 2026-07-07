package com.behaviorengine.core.domain.objects

/**
 * Lifecycle state of a taught [VisualObject]. Each has exactly one status color in the UI
 * (green/yellow/gray/red per this phase's spec) — see `VisualObjectStatus.toDisplayColor()` in
 * `core.presentation.objects`.
 */
enum class VisualObjectStatus {
    /** Fully taught and available; the default for a freshly created object. */
    READY,

    /** Deliberately turned off by the user; excluded from future recognition passes. */
    DISABLED,

    /** Mid-teaching — reserved for once teaching actually captures/processes images. */
    TRAINING,

    /** Retired but kept for reference rather than deleted. */
    ARCHIVED
}
