package com.thetheobot.shelfplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

@Composable
fun ItemDetailScreen(
    padding: PaddingValues,
    state: ItemDetailState,
    onBackClick: () -> Unit,
    onPlayClick: (String) -> Unit,
    onChapterSelected: (String) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Zurück")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        itemDetailStateTitle(state),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        itemDetailStateMessage(state),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        when (state) {
            ItemDetailState.Loading -> {
                item {
                    Text("Die Detailansicht wird geladen.")
                }
            }

            is ItemDetailState.Error -> {
                item {
                    Card(colors = CardDefaults.elevatedCardColors()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Etwas ist beim Laden der Details schiefgelaufen.")
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = onRetry) {
                                    Text("Nochmal versuchen")
                                }
                                OutlinedButton(onClick = onBackClick) {
                                    Text("Zur Bibliothek")
                                }
                            }
                        }
                    }
                }
            }

            is ItemDetailState.Loaded -> {
                val detail = state.detail
                val item = detail.item
                item {
                    DetailHeader(item = item)
                }
                item {
                    DetailDescription(description = detail.description)
                }
                item {
                    val firstChapter = detail.chapters.firstOrNull()
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = { onPlayClick(item.id) }) {
                            Text("Play/Pause")
                        }
                        OutlinedButton(
                            onClick = { firstChapter?.let { onChapterSelected(it.id) } },
                            enabled = firstChapter != null,
                        ) {
                            Text("Ab hier abspielen")
                        }
                        OutlinedButton(onClick = onBackClick) {
                            Text("Zurück")
                        }
                    }
                }
                item {
                    Text(
                        if (detail.chapters.isEmpty()) "Keine Kapitel vorhanden" else "Kapitel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (detail.chapters.isEmpty()) {
                    item {
                        Text(
                            "Für dieses Item wurden noch keine Kapitel geladen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    items(detail.chapters, key = { it.id }) { chapter ->
                        ChapterCard(
                            chapter = chapter,
                            onChapterSelected = { onChapterSelected(chapter.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(item: LibraryItem) {
    Card(colors = CardDefaults.elevatedCardColors()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.coverUrl?.isNotBlank() == true) {
                SubcomposeAsyncImage(
                    model = item.coverUrl,
                    contentDescription = "${item.title} Cover",
                    contentScale = ContentScale.Crop,
                    loading = { CoverFallback(item) },
                    error = { CoverFallback(item) },
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(18.dp)),
                )
            } else {
                CoverFallback(item)
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(item.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(item.author, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${formatItemType(item.itemType)} · Fortschritt ${formatProgress(item.progressPercent)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CoverFallback(item: LibraryItem) {
    val initials = item.title
        .trim()
        .take(2)
        .uppercase()
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            initials,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun DetailDescription(description: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val trimmed = description.trim()

    Card(colors = CardDefaults.elevatedCardColors()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Beschreibung", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (trimmed.isBlank()) "Keine Beschreibung vorhanden." else trimmed,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (trimmed.isNotBlank()) {
                OutlinedButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Weniger anzeigen" else "Mehr anzeigen")
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: LibraryChapter,
    onChapterSelected: () -> Unit,
) {
    Card(
        colors = CardDefaults.elevatedCardColors(),
        modifier = Modifier.clickable(onClick = onChapterSelected),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(chapter.title, style = MaterialTheme.typography.titleMedium)
            val chapterRange = formatChapterRange(chapter.startSeconds, chapter.endSeconds)
            if (chapterRange.isNotBlank()) {
                Text(
                    chapterRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onChapterSelected) {
                    Text("Ab hier abspielen")
                }
            }
        }
    }
}
