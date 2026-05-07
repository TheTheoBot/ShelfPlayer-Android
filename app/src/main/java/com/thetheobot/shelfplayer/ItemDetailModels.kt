package com.thetheobot.shelfplayer

data class LibraryChapter(
    val id: String,
    val title: String,
    val startSeconds: Int? = null,
    val endSeconds: Int? = null,
)

data class LibraryItemDetail(
    val item: LibraryItem,
    val progressPercent: Int,
    val description: String = "",
    val chapters: List<LibraryChapter> = emptyList(),
)

sealed interface ItemDetailState {
    data object Loading : ItemDetailState

    data class Loaded(
        val detail: LibraryItemDetail,
    ) : ItemDetailState

    data class Error(
        val message: String,
    ) : ItemDetailState
}

internal fun itemDetailStateTitle(state: ItemDetailState): String {
    return when (state) {
        ItemDetailState.Loading -> "Details werden geladen…"
        is ItemDetailState.Loaded -> state.detail.item.title
        is ItemDetailState.Error -> "Details konnten nicht geladen werden"
    }
}

internal fun itemDetailStateMessage(state: ItemDetailState): String {
    return when (state) {
        ItemDetailState.Loading -> "Die Detailansicht wird vorbereitet."
        is ItemDetailState.Loaded -> {
            val chapterCount = state.detail.chapters.size
            val chapterText = if (chapterCount == 1) "1 Kapitel" else "$chapterCount Kapitel"
            val progressText = "${state.detail.progressPercent}%"
            val description = state.detail.description.trim()
            if (description.isNotBlank()) {
                "$chapterText · $progressText · ${description.take(140)}"
            } else {
                "$chapterText · $progressText"
            }
        }
        is ItemDetailState.Error -> {
            if (state.message.isBlank()) {
                "Bitte später erneut versuchen."
            } else {
                state.message
            }
        }
    }
}

internal fun formatChapterRange(startSeconds: Int?, endSeconds: Int?): String {
    val start = startSeconds?.let(::formatChapterTime).orEmpty()
    val end = endSeconds?.let(::formatChapterTime).orEmpty()

    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start – $end"
        start.isNotBlank() -> start
        end.isNotBlank() -> end
        else -> ""
    }
}

private fun formatChapterTime(seconds: Int): String {
    val clampedSeconds = seconds.coerceAtLeast(0)
    val minutes = clampedSeconds / 60
    val remainingSeconds = clampedSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
