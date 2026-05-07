package com.thetheobot.shelfplayer

sealed interface LibraryFeedState {
    data object Loading : LibraryFeedState

    data class Loaded(
        val items: List<LibraryItem>,
    ) : LibraryFeedState

    data object Empty : LibraryFeedState

    data class Error(
        val message: String,
    ) : LibraryFeedState

    data class Refreshing(
        val items: List<LibraryItem>,
    ) : LibraryFeedState
}

fun libraryFeedStateOf(items: List<LibraryItem>): LibraryFeedState {
    val snapshot = items.toList()
    return if (snapshot.isEmpty()) {
        LibraryFeedState.Empty
    } else {
        LibraryFeedState.Loaded(snapshot)
    }
}

internal fun LibraryFeedState.visibleItems(): List<LibraryItem> {
    return when (this) {
        LibraryFeedState.Loading -> emptyList()
        LibraryFeedState.Empty -> emptyList()
        is LibraryFeedState.Error -> emptyList()
        is LibraryFeedState.Loaded -> items
        is LibraryFeedState.Refreshing -> items
    }
}

internal fun libraryFeedStateTitle(state: LibraryFeedState): String {
    return when (state) {
        LibraryFeedState.Loading -> "Bibliothek wird geladen…"
        LibraryFeedState.Empty -> "Noch keine Einträge vorhanden"
        is LibraryFeedState.Error -> "Bibliothek konnte nicht geladen werden"
        is LibraryFeedState.Loaded -> {
            val itemCount = state.items.size
            if (itemCount == 1) {
                "1 Eintrag verfügbar"
            } else {
                "$itemCount Einträge verfügbar"
            }
        }
        is LibraryFeedState.Refreshing -> {
            val itemCount = state.items.size
            if (itemCount == 1) {
                "Bibliothek wird aktualisiert · 1 Eintrag sichtbar"
            } else {
                "Bibliothek wird aktualisiert · $itemCount Einträge sichtbar"
            }
        }
    }
}

internal fun libraryFeedStateMessage(state: LibraryFeedState): String {
    return when (state) {
        LibraryFeedState.Loading -> "Audiobookshelf-Daten werden geladen."
        LibraryFeedState.Empty -> "Keine Inhalte gefunden. Prüfe Bibliothek und Filter im Server."
        is LibraryFeedState.Error -> {
            if (state.message.isBlank()) {
                "Bitte später erneut versuchen."
            } else {
                state.message
            }
        }
        is LibraryFeedState.Loaded -> "Titel, Autor und Fortschritt werden direkt vom Audiobookshelf-Server geladen."
        is LibraryFeedState.Refreshing -> "Vorhandene Einträge bleiben sichtbar, während neue Serverdaten nachgeladen werden."
    }
}