package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppRouteTest {
    @Test
    fun `item route parses item detail destination`() {
        assertEquals(
            AppRoute.ItemDetail(itemId = "abc123"),
            parseInternalAppRoute("item/abc123"),
        )
    }

    @Test
    fun `player route parses player destination`() {
        assertEquals(
            AppRoute.Player,
            parseInternalAppRoute("player"),
        )
    }

    @Test
    fun `unsupported or malformed routes return null`() {
        assertNull(parseInternalAppRoute(null))
        assertNull(parseInternalAppRoute(""))
        assertNull(parseInternalAppRoute("   "))
        assertNull(parseInternalAppRoute("item"))
        assertNull(parseInternalAppRoute("item/"))
        assertNull(parseInternalAppRoute("player/extra"))
        assertNull(parseInternalAppRoute("items/abc123"))
        assertNull(parseInternalAppRoute("Player"))
    }
}
