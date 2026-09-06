package com.orbin.minimal.feature.feed

import com.orbin.minimal.core.data.FeedSort
import com.orbin.minimal.core.model.FeedThread
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedEntriesTest {
    private fun thread(board: String, id: Long) =
        FeedThread(
            provider = "fourchan",
            board = board,
            threadId = id,
            title = "t$id",
            lastActivityEpochMillis = id,
            createdAtEpochMillis = id,
        )

    @Test
    fun `activity sort is a flat list of rows`() {
        val ordered = listOf(thread("a", 1), thread("b", 2))
        val entries = buildFeedEntries(FeedSort.ACTIVITY, ordered, emptyList(), "fourchan")
        assertEquals(2, entries.size)
        assertTrue(entries.all { it is FeedListEntry.Row })
    }

    @Test
    fun `board sort inserts headers then soft-caps total entries`() {
        val groups = listOf(
            "/a/" to List(10) { thread("a", it.toLong()) },
            "/b/" to List(10) { thread("b", 100L + it) },
        )
        val ordered = groups.flatMap { it.second }
        val capped = buildFeedEntries(
            sort = FeedSort.BOARD,
            ordered = ordered,
            groups = groups,
            selectedSiteId = "fourchan",
            maxItems = 5,
        )
        assertEquals(5, capped.size)
        assertTrue(capped.first() is FeedListEntry.Header)
    }
}
