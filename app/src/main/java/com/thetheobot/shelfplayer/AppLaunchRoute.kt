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

internal fun shouldApplyAppLaunchEvent(
    appliedLaunchEventId: Int,
    launchEventId: Int,
): Boolean {
    return launchEventId != appliedLaunchEventId
}

internal fun shouldIgnoreDeepLinkLaunch(
    initialRoute: AppRoute?,
    launchIntentIsDeepLink: Boolean,
): Boolean {
    return launchIntentIsDeepLink && initialRoute == null
}
