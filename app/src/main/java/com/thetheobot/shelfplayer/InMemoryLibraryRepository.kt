package com.thetheobot.shelfplayer

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InMemoryLibraryRepository(
    initialItems: List<LibraryItem> = defaultLibraryItems(),
) : LibraryRepository {
    private val seedItems = initialItems.toList()
    private val seedDetailsById = seedItems.associateBy({ it.id }, ::defaultLibraryDetail)
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

    override suspend fun getItemDetail(itemId: String): LibraryItemDetail? {
        return seedDetailsById[itemId]
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

private fun defaultLibraryDetail(item: LibraryItem): LibraryItemDetail {
    val chapters = when (item.id) {
        "project-hail-mary" -> listOf(
            LibraryChapter(id = "chapter-1", title = "Ein unerwarteter Aufbruch", startSeconds = 0, endSeconds = 900),
            LibraryChapter(id = "chapter-2", title = "Notfalllösungen", startSeconds = 900, endSeconds = 1_800),
            LibraryChapter(id = "chapter-3", title = "Ein neuer Verbündeter", startSeconds = 1_800, endSeconds = 2_700),
        )
        "der-name-des-windes" -> listOf(
            LibraryChapter(id = "chapter-1", title = "Ein Gasthaus am Ende der Welt", startSeconds = 0, endSeconds = 1_200),
            LibraryChapter(id = "chapter-2", title = "Namen und Geschichten", startSeconds = 1_200, endSeconds = 2_400),
        )
        "clean-architecture" -> listOf(
            LibraryChapter(id = "chapter-1", title = "Warum Architektur zählt", startSeconds = 0, endSeconds = 1_100),
            LibraryChapter(id = "chapter-2", title = "Die Architekturregel", startSeconds = 1_100, endSeconds = 2_100),
        )
        else -> listOf(
            LibraryChapter(id = "chapter-1", title = "Kapitel 1", startSeconds = 0, endSeconds = 900),
            LibraryChapter(id = "chapter-2", title = "Kapitel 2", startSeconds = 900, endSeconds = 1_800),
        )
    }

    return LibraryItemDetail(
        item = item,
        progressPercent = item.progressPercent,
        description = when (item.id) {
            "project-hail-mary" -> "Ryland Grace wacht allein auf einem Raumschiff auf und muss eine Mission retten, die über das Überleben der Menschheit entscheidet."
            "der-name-des-windes" -> "Kvothe erzählt seine Geschichte: von Magie, Musik, Verlust und dem Weg zu seinem eigenen Mythos."
            "clean-architecture" -> "Ein praxisnaher Leitfaden für wartbare Softwarearchitektur, Grenzen und Verantwortlichkeiten."
            else -> "Detailinformationen zu ${item.title} und ein erster Kapitelüberblick."
        },
        chapters = chapters,
    )
}
