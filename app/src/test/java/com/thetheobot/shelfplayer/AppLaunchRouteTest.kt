package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLaunchRouteTest {
    @Test
    fun `item detail route opens library tab with the item selected`() {
        assertEquals(
            AppLaunchSelection(
                tab = AppTab.Library,
                itemId = "abc123",
            ),
            appLaunchSelectionForRoute(AppRoute.ItemDetail(itemId = "abc123")),
        )
    }

    @Test
    fun `player route opens the player tab`() {
        assertEquals(
            AppLaunchSelection(tab = AppTab.Player),
            appLaunchSelectionForRoute(AppRoute.Player),
        )
    }

    @Test
    fun `null route does not request a special launch destination`() {
        assertNull(appLaunchSelectionForRoute(null))
    }
}
