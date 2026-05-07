package com.thetheobot.shelfplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsScreenTest {
    @Test
    fun `format skip interval renders seconds with suffix`() {
        assertEquals("15s", formatSkipInterval(15))
        assertEquals("60s", formatSkipInterval(60))
    }
}
