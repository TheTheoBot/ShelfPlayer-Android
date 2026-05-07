package com.thetheobot.shelfplayer

import kotlinx.coroutines.flow.StateFlow

interface LibraryRepository {
    val libraryItems: StateFlow<List<LibraryItem>>
}
