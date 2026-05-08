package com.thetheobot.shelfplayer

sealed interface SearchState {
    data object Idle : SearchState

    data class Typing(
        val query: String,
    ) : SearchState

    data class Searching(
        val query: String,
    ) : SearchState

    data class Results(
        val query: String,
        val items: List<LibraryItem>,
    ) : SearchState

    data class NoResults(
        val query: String,
    ) : SearchState

    data class Error(
        val query: String,
        val message: String,
    ) : SearchState
}

internal fun searchStateTitle(state: SearchState): String {
    return when (state) {
        SearchState.Idle -> "Suche in deiner Bibliothek"
        is SearchState.Typing -> "Suchbegriff eingeben"
        is SearchState.Searching -> "Suche läuft…"
        is SearchState.Results -> {
            val count = state.items.size
            if (count == 1) {
                "1 Treffer für \"${state.query}\""
            } else {
                "$count Treffer für \"${state.query}\""
            }
        }
        is SearchState.NoResults -> "Keine Treffer für \"${state.query}\""
        is SearchState.Error -> "Suche fehlgeschlagen"
    }
}

internal fun searchStateMessage(state: SearchState): String {
    return when (state) {
        SearchState.Idle -> "Suche nach Titel, Autor oder ID in der aktiven Bibliothek."
        is SearchState.Typing -> "Bestätige mit Suchen oder der Enter-Taste."
        is SearchState.Searching -> "Die aktive Bibliothek wird durchsucht."
        is SearchState.Results -> "Titel, Autor und ID werden lokal in der aktuellen Bibliothek gefiltert."
        is SearchState.NoResults -> "Keine Einträge passen zu diesem Suchbegriff."
        is SearchState.Error -> if (state.message.isBlank()) {
            "Bitte später erneut versuchen."
        } else {
            state.message
        }
    }
}

internal const val SEARCH_AUTOSUBMIT_DEBOUNCE_MS = 350L

internal fun normalizedSearchQuery(query: String): String {
    return query.trim()
}

internal fun shouldStartSearchRequest(
    query: String,
    activeSearchQuery: String? = null,
): Boolean {
    val normalizedQuery = normalizedSearchQuery(query)
    val activeNormalizedQuery = normalizedSearchQuery(activeSearchQuery.orEmpty())
    return normalizedQuery.isNotBlank() && activeNormalizedQuery != normalizedQuery
}

internal fun shouldAutoSubmitSearch(
    query: String,
    state: SearchState,
    activeSearchQuery: String? = null,
): Boolean {
    val normalizedQuery = normalizedSearchQuery(query)
    return shouldStartSearchRequest(normalizedQuery, activeSearchQuery) &&
        state is SearchState.Typing &&
        normalizedSearchQuery(state.query) == normalizedQuery
}

internal fun SearchState.canClearSearch(): Boolean {
    return when (this) {
        is SearchState.Typing -> normalizedSearchQuery(query).isNotBlank()
        is SearchState.Searching -> normalizedSearchQuery(query).isNotBlank()
        is SearchState.Results -> normalizedSearchQuery(query).isNotBlank()
        is SearchState.NoResults -> normalizedSearchQuery(query).isNotBlank()
        is SearchState.Error -> true
        else -> false
    }
}

internal fun SearchState.clearSearchActionLabel(): String? {
    return if (canClearSearch()) {
        "Suche löschen"
    } else {
        null
    }
}

internal fun SearchState.isRefreshErrorState(): Boolean {
    return this is SearchState.Error && normalizedSearchQuery(query).isBlank()
}

internal fun SearchState.resultsOrEmpty(): List<LibraryItem> {
    return when (this) {
        is SearchState.Results -> items
        else -> emptyList()
    }
}
