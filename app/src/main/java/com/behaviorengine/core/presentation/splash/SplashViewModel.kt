package com.behaviorengine.core.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.behaviorengine.core.domain.profile.UserProfileRepository
import com.behaviorengine.navigation.StartDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Decides Welcome-vs-Home exactly once per cold start, reading
 * [UserProfileRepository.awaitProfile] rather than the cached [UserProfileRepository.profile]
 * StateFlow — the latter's [kotlinx.coroutines.flow.SharingStarted.Eagerly] default would read
 * back `hasCompletedFirstLaunch = false` for a fraction of a second even for a returning user,
 * which is exactly the kind of flash-to-the-wrong-screen bug a splash screen exists to prevent.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val _startDestination = MutableStateFlow<StartDestination?>(null)
    val startDestination: StateFlow<StartDestination?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = userProfileRepository.awaitProfile()
            _startDestination.value = if (profile.hasCompletedFirstLaunch) {
                StartDestination.Home
            } else {
                StartDestination.Welcome
            }
        }
    }
}
