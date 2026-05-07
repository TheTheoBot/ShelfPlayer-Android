package com.thetheobot.shelfplayer

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun encodeUrlPathSegment(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
        .replace("+", "%20")
}
