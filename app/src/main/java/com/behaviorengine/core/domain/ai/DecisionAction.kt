package com.behaviorengine.core.domain.ai

import kotlinx.serialization.Serializable

/** Every action [DecisionEngine] can choose between, per spec's "possible outputs" list. */
@Serializable
enum class DecisionAction {
    CONTINUE_WORKFLOW,
    RETRY_STEP,
    SEARCH_ALTERNATIVE_OBJECT,
    SCROLL,
    WAIT,
    GO_BACK,
    RESTART_WORKFLOW,
    STOP_EXECUTION,
    ASK_USER_CONFIRMATION
}
