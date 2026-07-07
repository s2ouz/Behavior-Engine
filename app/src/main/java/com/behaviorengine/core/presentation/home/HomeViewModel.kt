package com.behaviorengine.core.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val NICKNAME_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * The product's navigation hub. Holds no engine state and no business logic — per this phase's
 * spec its four destinations are "navigation only" — it exists solely to greet the user by
 * [nickname] and let [HomeScreen] forward its own card taps to the nav graph.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    val nickname: StateFlow<String> = userProfileRepository.profile
        .map { it.nickname }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(NICKNAME_SUBSCRIPTION_TIMEOUT_MILLIS), "")
}
