package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class AudiobookshelfLibraryRepositoryTest {
    @Test
    fun `parseLibraryItems maps audiobookshelf payload to library items`() {
        val payload = """
            {
              "results": [
                {
                  "id": "item-1",
                  "title": "The Pragmatic Programmer",
                  "mediaType": "book",
                  "authorName": "Andrew Hunt",
                  "progress": 0.42,
                  "coverPath": "/api/items/item-1/cover"
                },
                {
                  "id": "item-2",
                  "title": "DevOps Radio",
                  "mediaType": "podcast",
                  "media": {
                    "authorName": "Jane Doe"
                  }
                }
              ]
            }
        """.trimIndent()

        val result = parseLibraryItems(payload, "http://abs.local")

        assertEquals(2, result.size)
        assertEquals("item-1", result[0].id)
        assertEquals("The Pragmatic Programmer", result[0].title)
        assertEquals("Andrew Hunt", result[0].author)
        assertEquals(42, result[0].progressPercent)
        assertEquals(LibraryItemType.Book, result[0].itemType)
        assertEquals("http://abs.local/api/items/item-1/cover", result[0].coverUrl)

        assertEquals("item-2", result[1].id)
        assertEquals("Jane Doe", result[1].author)
        assertEquals(0, result[1].progressPercent)
        assertEquals(LibraryItemType.Podcast, result[1].itemType)
        assertEquals("http://abs.local/api/items/item-2/cover", result[1].coverUrl)
    }

    @Test
    fun `parseLibraryItemDetail maps item payload to detail model with chapters`() {
        val payload = """
            {
              "id": "item-1",
              "title": "The Pragmatic Programmer",
              "mediaType": "book",
              "authorName": "Andrew Hunt",
              "description": "A classic software engineering book.",
              "progress": 0.42,
              "coverPath": "/api/items/item-1/cover",
              "chapters": [
                {
                  "id": "chapter-1",
                  "title": "Introduction",
                  "start": 0,
                  "end": 600
                },
                {
                  "id": "chapter-2",
                  "title": "Pragmatic Thinking",
                  "start": 600,
                  "end": 1200
                }
              ]
            }
        """.trimIndent()

        val result = parseLibraryItemDetail(payload, "http://abs.local")

        assertEquals("item-1", result.item.id)
        assertEquals("The Pragmatic Programmer", result.item.title)
        assertEquals(42, result.progressPercent)
        assertEquals("A classic software engineering book.", result.description)
        assertEquals(2, result.chapters.size)
        assertEquals("chapter-1", result.chapters.first().id)
        assertEquals("Introduction", result.chapters.first().title)
        assertEquals(0, result.chapters.first().startSeconds)
        assertEquals(600, result.chapters.first().endSeconds)
        assertEquals("http://abs.local/api/items/item-1/cover", result.item.coverUrl)
    }
}
