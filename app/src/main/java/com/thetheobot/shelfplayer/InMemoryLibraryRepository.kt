package com.thetheobot.shelfplayer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryLibraryRepository(
    initialItems: List<LibraryItem> = defaultLibraryItems(),
) : LibraryRepository {
    private val seedItems = initialItems.toList()
    private val _libraryFeedState = MutableStateFlow<LibraryFeedState>(LibraryFeedState.Loading)

    override val libraryFeedState: StateFlow<LibraryFeedState> = _libraryFeedState

    override suspend fun refresh() {
        val currentState = _libraryFeedState.value
        val visibleItems = currentState.visibleItems().ifEmpty { seedItems }
        _libraryFeedState.value = LibraryFeedState.Refreshing(visibleItems)
        try {
            delay(120)
        } finally {
            _libraryFeedState.value = libraryFeedStateOf(visibleItems)
        }
    }
}

private fun defaultLibraryItems(): List<LibraryItem> {
    return listOf(
        LibraryItem(
            id = "der-name-des-windes",
            title = "Der Name des Windes",
            author = "Patrick Rothfuss",
            progressPercent = 47,
            itemType = LibraryItemType.Audiobook,
            coverUrl = "https://picsum.photos/seed/der-name-des-windes/240/240",
        ),
        LibraryItem(
            id = "project-hail-mary",
            title = "Project Hail Mary",
            author = "Andy Weir",
            progressPercent = 82,
            itemType = LibraryItemType.Audiobook,
            coverUrl = "https://picsum.photos/seed/project-hail-mary/240/240",
        ),
        LibraryItem(
            id = "die-verwandlung",
            title = "Die Verwandlung",
            author = "Franz Kafka",
            progressPercent = 0,
            itemType = LibraryItemType.Book,
            coverUrl = "https://picsum.photos/seed/die-verwandlung/240/240",
        ),
        LibraryItem(
            id = "clean-architecture",
            title = "Clean Architecture",
            author = "Robert C. Martin",
            progressPercent = 23,
            itemType = LibraryItemType.Book,
            coverUrl = "https://picsum.photos/seed/clean-architecture/240/240",
        ),
    )
}
