package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryRepositoryTest {
    @Test
    fun `in memory repository exposes typed sample library items`() {
        val repository = InMemoryLibraryRepository()
        val items = repository.libraryItems.value

        assertTrue(items.isNotEmpty())
        assertEquals("Der Name des Windes", items.first().title)
        assertEquals(LibraryItemType.Audiobook, items.first().itemType)
    }

    @Test
    fun `in memory repository copies the initial item list defensively`() {
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

        assertEquals(1, repository.libraryItems.value.size)
        assertEquals("example-id", repository.libraryItems.value.single().id)
    }

    @Test
    fun `in memory repository supports empty custom content`() {
        val repository = InMemoryLibraryRepository(initialItems = emptyList())

        assertTrue(repository.libraryItems.value.isEmpty())
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
    fun `library screen helpers clamp progress and translate item types`() {
        assertEquals("0%", formatProgress(-4))
        assertEquals("100%", formatProgress(103))
        assertEquals("Podcast", formatItemType(LibraryItemType.Podcast))
        assertEquals("Serie", formatItemType(LibraryItemType.Series))
    }
}
