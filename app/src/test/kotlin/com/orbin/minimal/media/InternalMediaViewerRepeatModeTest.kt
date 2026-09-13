package com.orbin.minimal.media

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class InternalMediaViewerRepeatModeTest {
    @Test
    fun `loop enabled repeats current media`() {
        assertEquals(Player.REPEAT_MODE_ONE, repeatModeFor(true))
    }

    @Test
    fun `loop disabled plays current media once`() {
        assertEquals(Player.REPEAT_MODE_OFF, repeatModeFor(false))
    }
}
