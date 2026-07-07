package com.behaviorengine.core.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.behaviorengine.R

/**
 * The product's landing screen: a greeting and four large navigation cards
 * (Objects/Teaching/Automation/Settings). Deliberately inert beyond navigation this phase — no
 * card shows live data yet, since Objects/Teaching/Automation have nothing behind them until
 * later phases implement recognition, teaching, and automation execution.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToObjects: () -> Unit,
    onNavigateToTeaching: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val nickname by viewModel.nickname.collectAsState()

    HomeContent(
        nickname = nickname,
        onNavigateToObjects = onNavigateToObjects,
        onNavigateToTeaching = onNavigateToTeaching,
        onNavigateToAutomation = onNavigateToAutomation,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
private fun HomeContent(
    nickname: String,
    onNavigateToObjects: () -> Unit,
    onNavigateToTeaching: () -> Unit,
    onNavigateToAutomation: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 24.dp, vertical = 48.dp)),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.home_greeting, nickname),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        HomeCard(
            title = stringResource(R.string.home_card_objects_title),
            description = stringResource(R.string.home_card_objects_description),
            onClick = onNavigateToObjects
        )
        HomeCard(
            title = stringResource(R.string.home_card_teaching_title),
            description = stringResource(R.string.home_card_teaching_description),
            onClick = onNavigateToTeaching
        )
        HomeCard(
            title = stringResource(R.string.home_card_automation_title),
            description = stringResource(R.string.home_card_automation_description),
            onClick = onNavigateToAutomation
        )
        HomeCard(
            title = stringResource(R.string.home_card_settings_title),
            description = stringResource(R.string.home_card_settings_description),
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun HomeCard(title: String, description: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
