package com.behaviorengine.core.domain.ai

import kotlinx.serialization.Serializable

/** Coarse screen classification [StateRecognitionEngine] assigns to a live capture. */
@Serializable
enum class ScreenType {
    LOGIN,
    HOME,
    SETTINGS,
    LOADING,
    ERROR,
    CONFIRMATION_DIALOG,
    PERMISSION_DIALOG,
    UNKNOWN
}
