package com.thetheobot.shelfplayer

internal fun formatLibraryItemMetadata(item: LibraryItem): String {
    return "${formatItemType(item.itemType)} · ${formatProgress(item.progressPercent)}"
}
