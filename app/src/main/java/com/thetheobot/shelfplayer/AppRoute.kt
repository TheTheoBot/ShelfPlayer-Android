package com.thetheobot.shelfplayer

sealed interface AppRoute {
    data class ItemDetail(
        val itemId: String,
    ) : AppRoute

    data object Player : AppRoute
}

internal fun parseInternalAppRoute(route: String?): AppRoute? {
    val normalizedRoute = route
        ?.trim()
        ?.removePrefix("/")
        .orEmpty()

    if (normalizedRoute.isBlank()) {
        return null
    }

    return when {
        normalizedRoute == "player" -> AppRoute.Player
        normalizedRoute.startsWith("item/") -> {
            val itemId = normalizedRoute.removePrefix("item/")
            if (itemId.isBlank() || itemId.contains('/')) {
                null
            } else {
                AppRoute.ItemDetail(itemId = itemId)
            }
        }
        else -> null
    }
}
