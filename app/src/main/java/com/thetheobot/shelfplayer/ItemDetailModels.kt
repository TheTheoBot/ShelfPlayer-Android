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

internal fun selectedChapterDisplayLabel(
    chapters: List<LibraryChapter>,
    selectedChapterId: String?,
): String? {
    val chapterId = selectedChapterId?.trim().orEmpty()
    if (chapterId.isBlank()) {
        return null
    }

    val chapter = chapters.firstOrNull { it.id == chapterId } ?: return "Ausgewähltes Kapitel"
    val chapterRange = formatChapterRange(chapter.startSeconds, chapter.endSeconds)

    return listOfNotNull(
        chapter.title.trim().takeIf { it.isNotBlank() },
        chapterRange.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { "Ausgewähltes Kapitel" }
}

internal data class PlayerSelectedChapterContext(
    val label: String?,
    val startSeconds: Int?,
)

internal fun resolvePlayerSelectedChapterContext(
    playbackActiveItemId: String?,
    playbackLibraryItemDetail: LibraryItemDetail?,
    selectedChapterId: String?,
    selectedChapterStartSeconds: Int?,
): PlayerSelectedChapterContext {
    if (!isSelectedChapterContextActivePlaybackItem(
            playbackActiveItemId = playbackActiveItemId,
            selectedItemId = playbackLibraryItemDetail?.item?.id,
        )
    ) {
        return PlayerSelectedChapterContext(label = null, startSeconds = null)
    }

    val selectedChapterLabel = playbackLibraryItemDetail?.chapters.let { chapters ->
        selectedChapterId?.let { chapterId -> selectedChapterDisplayLabel(chapters.orEmpty(), chapterId) }
    }

    return PlayerSelectedChapterContext(
        label = selectedChapterLabel,
        startSeconds = selectedChapterStartSeconds,
    )
}

internal fun activeChapterDisplayLabel(chapter: LibraryChapter?): String {
    val resolvedChapter = chapter ?: return "Kein Kapitel an dieser Position"
    val chapterRange = formatChapterRange(resolvedChapter.startSeconds, resolvedChapter.endSeconds)

    return listOfNotNull(
        resolvedChapter.title.trim().takeIf { it.isNotBlank() } ?: "Unbenanntes Kapitel",
        chapterRange.takeIf { it.isNotBlank() },
    ).joinToString(" · ")
}

internal fun activeChapterContextDisplayText(chapter: LibraryChapter?): String {
    return "Aktuelles Kapitel: ${activeChapterDisplayLabel(chapter)}"
}

internal fun isSelectedChapterContextActivePlaybackItem(
    playbackActiveItemId: String?,
    selectedItemId: String?,
): Boolean {
    val activeItemId = playbackActiveItemId?.trim().orEmpty()
    val resolvedSelectedItemId = selectedItemId?.trim().orEmpty()
    return activeItemId.isNotBlank() && activeItemId == resolvedSelectedItemId
}

internal fun shouldResetSelectedChapterContext(
    previousPlaybackItemId: String?,
    currentPlaybackItemId: String?,
): Boolean {
    val previousItemId = previousPlaybackItemId?.trim().orEmpty()
    val currentItemId = currentPlaybackItemId?.trim().orEmpty()
    return previousItemId.isNotBlank() && previousItemId != currentItemId
}

internal fun resolveActiveChapterForPlaybackPosition(
    chapters: List<LibraryChapter>,
    playbackPositionMs: Int,
): LibraryChapter? {
    if (playbackPositionMs < 0) {
        return null
    }

    val validChapters = chapters.mapIndexedNotNull { index, chapter ->
        val startSeconds = chapter.startSeconds?.coerceAtLeast(0) ?: return@mapIndexedNotNull null
        ChapterWithStart(index = index, chapter = chapter, startMs = startSeconds.toLong() * 1000L)
    }.sortedWith(
        compareBy<ChapterWithStart> { it.startMs }
            .thenBy { it.index },
    )

    if (validChapters.isEmpty()) {
        return null
    }

    val playbackPositionMsLong = playbackPositionMs.toLong()

    validChapters.forEachIndexed { index, current ->
        val nextStartMs = validChapters.getOrNull(index + 1)?.startMs
        val chapterEndMs = current.chapter.endSeconds
            ?.coerceAtLeast(0)
            ?.toLong()
            ?.times(1000L)
            ?.let { endMs -> nextStartMs?.let { minOf(endMs, it) } ?: endMs }
            ?: nextStartMs

        val isWithinChapter = playbackPositionMsLong >= current.startMs &&
            (chapterEndMs == null || playbackPositionMsLong < chapterEndMs)
        if (isWithinChapter) {
            return current.chapter
        }
    }

    return null
}

private data class ChapterWithStart(
    val index: Int,
    val chapter: LibraryChapter,
    val startMs: Long,
)

private fun formatChapterTime(seconds: Int): String {
    val clampedSeconds = seconds.coerceAtLeast(0)
    val minutes = clampedSeconds / 60
    val remainingSeconds = clampedSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}
