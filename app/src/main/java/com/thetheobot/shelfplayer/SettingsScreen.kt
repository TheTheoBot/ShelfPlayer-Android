package com.thetheobot.shelfplayer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val playbackRateOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f)
private val playbackSkipIntervalOptions = listOf(5, 10, 15, 30, 45, 60)

internal fun formatSkipInterval(seconds: Int): String = "${seconds}s"

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    appSettingsRepository: AppSettingsRepository,
) {
    val settings by appSettingsRepository.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Einstellungen",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Diese Werte werden für neue Wiedergaben und die Player-Steuerung verwendet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(colors = CardDefaults.elevatedCardColors()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Standard-Wiedergabegeschwindigkeit", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Neue Wiedergaben starten mit dieser Geschwindigkeit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsChoiceRow(
                    selectedValue = settings.defaultPlaybackRate,
                    options = playbackRateOptions,
                    optionLabel = ::formatPlaybackRate,
                    onSelected = { appSettingsRepository.setDefaultPlaybackRate(it) },
                )
            }
        }

        Card(colors = CardDefaults.elevatedCardColors()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Sprungintervall", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Die Skip-Buttons im Player verwenden dieses Intervall.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsChoiceRow(
                    selectedValue = settings.playbackSkipIntervalSeconds,
                    options = playbackSkipIntervalOptions,
                    optionLabel = ::formatSkipInterval,
                    onSelected = { appSettingsRepository.setPlaybackSkipIntervalSeconds(it) },
                )
            }
        }
    }
}

@Composable
private fun <T> SettingsChoiceRow(
    selectedValue: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { optionRow ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                optionRow.forEach { option ->
                    val selected = option == selectedValue
                    val label = optionLabel(option)
                    if (selected) {
                        Button(onClick = { onSelected(option) }) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(onClick = { onSelected(option) }) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}
