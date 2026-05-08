package com.thetheobot.shelfplayer

internal data class AppLaunchSelection(
    val tab: AppTab,
    val itemId: String? = null,
)

internal fun appLaunchSelectionForRoute(route: AppRoute?): AppLaunchSelection? {
    return when (route) {
        is AppRoute.ItemDetail -> AppLaunchSelection(
            tab = AppTab.Library,
            itemId = route.itemId,
        )
        AppRoute.Player -> AppLaunchSelection(tab = AppTab.Player)
        null -> null
    }
}
