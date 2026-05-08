package com.thetheobot.shelfplayer

internal fun searchLibraryItems(
    items: List<LibraryItem>,
    query: String,
): List<LibraryItem> {
    val normalizedTokens = query
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

    if (normalizedTokens.isEmpty()) {
        return emptyList()
    }

    return items.filter { item ->
        normalizedTokens.all { token ->
            item.id.contains(token, ignoreCase = true) ||
                item.title.contains(token, ignoreCase = true) ||
                item.author.contains(token, ignoreCase = true)
        }
    }
}
