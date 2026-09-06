package com.orbin.minimal.core.data

import com.orbin.minimal.core.model.BoardRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowedBoardStoreTest {
    @Test
    fun `parse and serialize round-trip`() {
        val boards = listOf(
            BoardRef("fourchan", "g", "Technology"),
            BoardRef("bbwchan", "b", "b"),
        )
        val raw = FollowedBoardStore.serializeBoards(boards)
        assertEquals(boards, FollowedBoardStore.parseBoards(raw))
    }

    @Test
    fun `all uses in-memory cache until write invalidates`() {
        var reads = 0
        var stored = "[]"
        val store = FollowedBoardStore(
            readRaw = {
                reads++
                stored
            },
            writeRaw = { stored = it },
        )

        assertEquals(emptyList<BoardRef>(), store.all())
        assertEquals(1, reads)
        assertEquals(emptyList<BoardRef>(), store.all())
        assertEquals(1, reads) // cached — no second prefs read

        assertTrue(store.toggle(BoardRef("fourchan", "g", "Technology")))
        assertEquals(1, reads) // save writes cache directly without re-read
        assertEquals(listOf(BoardRef("fourchan", "g", "Technology")), store.all())
        assertEquals(1, reads)

        assertTrue(store.isFollowed(BoardRef("fourchan", "g")))
        assertEquals(1, reads)

        assertFalse(store.toggle(BoardRef("fourchan", "g", "Technology")))
        assertEquals(emptyList<BoardRef>(), store.all())
        assertEquals(1, reads)
    }

    @Test
    fun `rejects invalid board slugs on parse`() {
        val raw = """[{"provider":"fourchan","board":"../evil","title":"x"}]"""
        assertEquals(emptyList<BoardRef>(), FollowedBoardStore.parseBoards(raw))
    }
}
