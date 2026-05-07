package com.thetheobot.shelfplayer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryLibraryRepository(
    initialItems: List<LibraryItem> = defaultLibraryItems(),
) : LibraryRepository {
    private val _libraryItems = MutableStateFlow(initialItems.toList())

    override val libraryItems: StateFlow<List<LibraryItem>> = _libraryItems
}

private fun defaultLibraryItems(): List<LibraryItem> {
    return listOf(
        LibraryItem(
            id = "der-name-des-windes",
            title = "Der Name des Windes",
            author = "Patrick Rothfuss",
            progressPercent = 47,
            itemType = LibraryItemType.Audiobook,
        ),
        LibraryItem(
            id = "project-hail-mary",
            title = "Project Hail Mary",
            author = "Andy Weir",
            progressPercent = 82,
            itemType = LibraryItemType.Audiobook,
        ),
        LibraryItem(
            id = "die-verwandlung",
            title = "Die Verwandlung",
            author = "Franz Kafka",
            progressPercent = 0,
            itemType = LibraryItemType.Book,
        ),
        LibraryItem(
            id = "clean-architecture",
            title = "Clean Architecture",
            author = "Robert C. Martin",
            progressPercent = 23,
            itemType = LibraryItemType.Book,
        ),
    )
}
