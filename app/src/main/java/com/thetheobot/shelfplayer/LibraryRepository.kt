package com.thetheobot.shelfplayer

import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val libraryFeedState: StateFlow<LibraryFeedState>

    suspend fun refresh()
}
