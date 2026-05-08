package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `runtime route updates apply for distinct launch events`() {
        assertTrue(shouldApplyAppLaunchEvent(-1, 1))
        assertTrue(shouldApplyAppLaunchEvent(1, 2))
    }

    @Test
    fun `runtime route updates ignore duplicate launch events`() {
        assertFalse(shouldApplyAppLaunchEvent(2, 2))
    }

    @Test
    fun `invalid deep-link routes are ignored while launcher resets are allowed`() {
        assertTrue(shouldIgnoreDeepLinkLaunch(null, true))
        assertFalse(shouldIgnoreDeepLinkLaunch(AppRoute.Player, true))
        assertFalse(shouldIgnoreDeepLinkLaunch(null, false))
    }

    @Test
    fun `null route does not request a special launch destination`() {
        assertNull(appLaunchSelectionForRoute(null))
    }
}
