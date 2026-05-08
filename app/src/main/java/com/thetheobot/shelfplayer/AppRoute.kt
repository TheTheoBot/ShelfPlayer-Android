package com.thetheobot.shelfplayer

import java.io.ByteArrayOutputStream

sealed interface AppRoute {
    data class ItemDetail(
        val itemId: String,
    ) : AppRoute

    data object Player : AppRoute
}

internal fun parseInternalAppRoute(route: String?): AppRoute? {
    val normalizedRoute = route
        ?.trim()
        ?.trimStart('/')
        .orEmpty()

    if (normalizedRoute.isBlank()) {
        return null
    }

    val segments = normalizedRoute.split('/')

    return when (segments[0]) {
        "player" -> if (segments.size == 1) AppRoute.Player else null
        "item" -> {
            if (segments.size != 2) {
                null
            } else {
                val itemId = decodePercentEncodedPathSegment(segments[1])
                if (itemId.isNullOrBlank() || itemId.contains('/')) {
                    null
                } else {
                    AppRoute.ItemDetail(itemId = itemId)
                }
            }
        }
        else -> null
    }
}

private fun decodePercentEncodedPathSegment(segment: String): String? {
    val decoded = StringBuilder(segment.length)
    val bytes = ByteArrayOutputStream(segment.length)
    var index = 0

    fun flushBytes() {
        if (bytes.size() > 0) {
            decoded.append(String(bytes.toByteArray(), Charsets.UTF_8))
            bytes.reset()
        }
    }

    while (index < segment.length) {
        val current = segment[index]
        if (current == '%') {
            if (index + 2 >= segment.length) {
                return null
            }

            val high = Character.digit(segment[index + 1], 16)
            val low = Character.digit(segment[index + 2], 16)
            if (high < 0 || low < 0) {
                return null
            }
            bytes.write((high shl 4) + low)
            index += 3
        } else {
            flushBytes()
            decoded.append(current)
            index++
        }
    }

    flushBytes()
    return decoded.toString()
}
