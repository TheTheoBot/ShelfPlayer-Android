package com.thetheobot.shelfplayer

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.CharacterCodingException

sealed interface AppRoute {
    data class ItemDetail(
        val itemId: String,
    ) : AppRoute

    data object Player : AppRoute
}

internal fun parseInternalAppRoute(route: String?): AppRoute? {
    val normalizedRoute = normalizeInternalRoute(route)

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

private fun normalizeInternalRoute(route: String?): String {
    val trimmedRoute = route?.trim()?.trimStart('/').orEmpty()
    val suffixStart = trimmedRoute.indexOfAny(charArrayOf('?', '#'))
    return if (suffixStart >= 0) trimmedRoute.substring(0, suffixStart) else trimmedRoute
}

private fun decodePercentEncodedPathSegment(segment: String): String? {
    val decoded = StringBuilder(segment.length)
    val bytes = ByteArrayOutputStream(segment.length)
    var index = 0

    fun flushBytes(): Boolean {
        if (bytes.size() == 0) {
            return true
        }

        val decodedBytes = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes.toByteArray()))
        } catch (_: CharacterCodingException) {
            return false
        }

        decoded.append(decodedBytes)
        bytes.reset()
        return true
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
            if (!flushBytes()) {
                return null
            }
            decoded.append(current)
            index++
        }
    }

    if (!flushBytes()) {
        return null
    }
    return decoded.toString()
}
