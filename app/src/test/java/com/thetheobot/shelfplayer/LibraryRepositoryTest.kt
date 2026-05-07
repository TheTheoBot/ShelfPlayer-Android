package com.thetheobot.shelfplayer

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {
    @Test
    fun `in memory repository exposes loading before the first refresh and then typed sample library feed state`() = runBlocking {
        val repository = InMemoryLibraryRepository()

        assertEquals(LibraryFeedState.Loading, repository.libraryFeedState.value)

        repository.refresh()

        val state = repository.libraryFeedState.value
        assertTrue(state is LibraryFeedState.Loaded)
        val items = (state as LibraryFeedState.Loaded).items
        assertTrue(items.isNotEmpty())
        assertEquals("Der Name des Windes", items.first().title)
        assertEquals(LibraryItemType.Audiobook, items.first().itemType)
    }

    @Test
    fun `in memory repository copies the initial item list defensively`() = runBlocking {
        val mutableItems = mutableListOf(
            LibraryItem(
                id = "example-id",
                title = "Example Title",
                author = "Example Author",
                progressPercent = 12,
                itemType = LibraryItemType.Book,
            ),
        )

        val repository = InMemoryLibraryRepository(initialItems = mutableItems)
        mutableItems += LibraryItem(
            id = "mutated-id",
            title = "Mutated Title",
            author = "Mutated Author",
            progressPercent = 88,
            itemType = LibraryItemType.Podcast,
        )

        assertEquals(LibraryFeedState.Loading, repository.libraryFeedState.value)

        repository.refresh()

        val state = repository.libraryFeedState.value
        assertTrue(state is LibraryFeedState.Loaded)
        assertEquals(1, (state as LibraryFeedState.Loaded).items.size)
        assertEquals("example-id", state.items.single().id)
    }

    @Test
    fun `in memory repository supports empty custom content`() = runBlocking {
        val repository = InMemoryLibraryRepository(initialItems = emptyList())

        assertEquals(LibraryFeedState.Loading, repository.libraryFeedState.value)

        repository.refresh()

        assertEquals(LibraryFeedState.Empty, repository.libraryFeedState.value)
    }

    @Test
    fun `library feed helper maps empty and non empty lists to the expected states`() {
        val sampleItem = LibraryItem(
            id = "example-id",
            title = "Example Title",
            author = "Example Author",
            progressPercent = 12,
            itemType = LibraryItemType.Book,
        )

        assertEquals(LibraryFeedState.Empty, libraryFeedStateOf(emptyList()))
        assertEquals(LibraryFeedState.Loaded(listOf(sampleItem)), libraryFeedStateOf(listOf(sampleItem)))
        assertEquals(
            "1 Eintrag verfügbar",
            libraryFeedStateTitle(LibraryFeedState.Loaded(listOf(sampleItem))),
        )
        assertEquals(
            "Titel, Autor und Fortschritt werden direkt vom Audiobookshelf-Server geladen.",
            libraryFeedStateMessage(LibraryFeedState.Loaded(listOf(sampleItem))),
        )
    }

    @Test
    fun `library feed helper keeps refreshing items visible and exposes copy`() {
        val refreshingState = LibraryFeedState.Refreshing(
            items = listOf(
                LibraryItem(
                    id = "refresh-id",
                    title = "Refreshing Title",
                    author = "Refreshing Author",
                    progressPercent = 66,
                    itemType = LibraryItemType.Podcast,
                ),
            ),
        )

        assertEquals(listOf("refresh-id"), refreshingState.visibleItems().map { it.id })
        assertEquals("Bibliothek wird aktualisiert · 1 Eintrag sichtbar", libraryFeedStateTitle(refreshingState))
        assertEquals(
            "Vorhandene Einträge bleiben sichtbar, während neue Serverdaten nachgeladen werden.",
            libraryFeedStateMessage(refreshingState),
        )
    }

    @Test
    fun `library feed helper exposes stable copy for loading copy`() {
        assertEquals("Bibliothek wird geladen…", libraryFeedStateTitle(LibraryFeedState.Loading))
        assertEquals(
            "Audiobookshelf-Daten werden geladen.",
            libraryFeedStateMessage(LibraryFeedState.Loading),
        )
        assertEquals(
            "Bitte später erneut versuchen.",
            libraryFeedStateMessage(LibraryFeedState.Error(message = "   ")),
        )
    }

    @Test
    fun `library item model keeps the expected fields`() {
        val item = LibraryItem(
            id = "example-id",
            title = "Example Title",
            author = "Example Author",
            progressPercent = 12,
            itemType = LibraryItemType.Book,
            coverUrl = "https://example.com/cover.jpg",
        )

        assertEquals("example-id", item.id)
        assertEquals("Example Title", item.title)
        assertEquals("Example Author", item.author)
        assertEquals(12, item.progressPercent)
        assertEquals(LibraryItemType.Book, item.itemType)
        assertEquals("https://example.com/cover.jpg", item.coverUrl)
    }

    @Test
    fun `in memory repository refresh exposes a refreshing state before restoring the sample feed`() = runBlocking {
        val repository = InMemoryLibraryRepository()
        val refreshJob = launch { repository.refresh() }

        yield()
        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Refreshing)

        refreshJob.join()
        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Loaded)
    }

    @Test
    fun `in memory repository refresh recovers from cancellation without getting stuck refreshing`() = runBlocking {
        val repository = InMemoryLibraryRepository()
        val refreshJob = launch { repository.refresh() }

        yield()
        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Refreshing)

        refreshJob.cancel()
        refreshJob.join()
        assertTrue(repository.libraryFeedState.value is LibraryFeedState.Loaded)
    }

    @Test
    fun `library screen helpers clamp progress and translate item types`() {
        assertEquals("0%", formatProgress(-4))
        assertEquals("100%", formatProgress(103))
        assertEquals("Podcast", formatItemType(LibraryItemType.Podcast))
        assertEquals("Serie", formatItemType(LibraryItemType.Series))
    }
}
