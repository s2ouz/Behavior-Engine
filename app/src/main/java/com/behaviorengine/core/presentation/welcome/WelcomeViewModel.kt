package com.behaviorengine.core.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NICKNAME_SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L

/**
 * Drives onboarding: validates the nickname, then persists it via [UserProfileRepository] and
 * marks first launch complete in one action. [WelcomeScreen] observes [onboardingComplete] and
 * navigates once it flips to true — this class never touches a `NavController` itself, keeping
 * navigation a UI concern.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _onboardingComplete = MutableStateFlow(false)
    val onboardingComplete: StateFlow<Boolean> = _onboardingComplete.asStateFlow()

    val isNicknameValid: StateFlow<Boolean> = nickname
        .map { it.trim().length in NICKNAME_MIN_LENGTH..NICKNAME_MAX_LENGTH }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(NICKNAME_SUBSCRIPTION_TIMEOUT_MILLIS), false)

    init {
        // Pre-fills a nickname saved by a previous attempt that was interrupted before
        // completeFirstLaunch() ran (e.g. process death right after typing) — a cheap
        // robustness win, not a feature: without it that user would just retype it.
        viewModelScope.launch {
            val existing = userProfileRepository.awaitProfile().nickname
            if (existing.isNotEmpty()) _nickname.value = existing
        }
    }

    fun onNicknameChanged(value: String) {
        if (value.length <= NICKNAME_MAX_LENGTH) {
            _nickname.value = value
        }
    }

    fun onContinueClicked() {
        if (!isNicknameValid.value) return
        viewModelScope.launch {
            userProfileRepository.saveNickname(nickname.value.trim())
            userProfileRepository.completeFirstLaunch()
            _onboardingComplete.value = true
        }
    }

    companion object {
        const val NICKNAME_MIN_LENGTH = 3
        const val NICKNAME_MAX_LENGTH = 20
    }
}
