package com.thetheobot.shelfplayer

enum class LibraryItemType {
    Audiobook,
    Book,
    Podcast,
    Series,
}

data class LibraryItem(
    val id: String,
    val title: String,
    val author: String,
    val progressPercent: Int,
    val itemType: LibraryItemType,
    val coverUrl: String? = null,
)
