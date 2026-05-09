package com.thetheobot.shelfplayer

internal fun formatLibraryItemMetadata(item: LibraryItem): String {
    return formatLibraryItemSummary(item)
}

internal fun formatLibraryItemSummary(item: LibraryItem): String {
    return "${formatItemType(item.itemType)} · ${formatProgress(item.progressPercent)}"
}

internal fun libraryItemPrimaryActionLabel(item: LibraryItem): String {
    val progressPercent = item.progressPercent.coerceIn(0, 100)
    return when {
        progressPercent >= 100 -> "Nochmal abspielen"
        progressPercent > 0 -> "Fortsetzen"
        else -> "Jetzt abspielen"
    }
}

internal fun libraryItemSecondaryActionLabel(): String {
    return "Details ansehen"
}
