package com.behaviorengine.core.presentation.welcome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R
import com.behaviorengine.core.common.AppConstants

private const val ENTRANCE_ANIMATION_MILLIS = 500

/**
 * First-launch onboarding: the entire "account system" this product will ever have is a local
 * nickname — no login, no email, no network call. [WelcomeViewModel.onContinueClicked] persists
 * it and marks onboarding complete; this screen only reacts to that and navigates onward.
 */
@Composable
fun WelcomeScreen(viewModel: WelcomeViewModel = hiltViewModel(), onOnboardingComplete: () -> Unit) {
    val nickname by viewModel.nickname.collectAsState()
    val isNicknameValid by viewModel.isNicknameValid.collectAsState()
    val onboardingComplete by viewModel.onboardingComplete.collectAsState()

    LaunchedEffect(onboardingComplete) {
        if (onboardingComplete) onOnboardingComplete()
    }

    WelcomeContent(
        nickname = nickname,
        isNicknameValid = isNicknameValid,
        onNicknameChanged = viewModel::onNicknameChanged,
        onContinueClicked = viewModel::onContinueClicked
    )
}

@Composable
private fun WelcomeContent(
    nickname: String,
    isNicknameValid: Boolean,
    onNicknameChanged: (String) -> Unit,
    onContinueClicked: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(ENTRANCE_ANIMATION_MILLIS)) +
                slideInVertically(tween(ENTRANCE_ANIMATION_MILLIS)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(horizontal = 32.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = AppConstants.PROJECT_NAME,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.welcome_subtitle),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.welcome_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = onNicknameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.welcome_nickname_placeholder)) },
                    singleLine = true,
                    isError = nickname.isNotEmpty() && !isNicknameValid,
                    supportingText = {
                        Text(
                            text = stringResource(
                                R.string.welcome_nickname_helper,
                                WelcomeViewModel.NICKNAME_MIN_LENGTH,
                                WelcomeViewModel.NICKNAME_MAX_LENGTH
                            )
                        )
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onContinueClicked,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isNicknameValid
                ) {
                    Text(stringResource(R.string.welcome_continue_button))
                }
            }
        }
    }
}
