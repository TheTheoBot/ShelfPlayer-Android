package com.thetheobot.shelfplayer

import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val libraryFeedState: StateFlow<LibraryFeedState>

    suspend fun refresh()

    suspend fun search(query: String): List<LibraryItem>

    suspend fun getItemDetail(itemId: String): LibraryItemDetail?
}
