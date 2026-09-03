package com.orbin.minimal.core.data

import com.orbin.minimal.core.model.FeedThread
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedSortTest {

    private fun thread(
        provider: String = "vichan",
        board: String,
        id: Long,
        created: Long,
        activity: Long = created,
    ) = FeedThread(
        provider = provider,
        board = board,
        threadId = id,
        title = "t$id",
        lastActivityEpochMillis = activity,
        createdAtEpochMillis = created,
    )

    @Test
    fun `board sort is the default`() {
        assertEquals(FeedSort.BOARD, FeedSort.DEFAULT)
    }

    @Test
    fun `board sort groups by board and orders newest thread first`() {
        val feed = listOf(
            thread(board = "b", id = 1, created = 100),
            thread(board = "a", id = 2, created = 100),
            thread(board = "b", id = 3, created = 300),
            thread(board = "a", id = 4, created = 300),
        )

        assertEquals(
            listOf(4L, 2L, 3L, 1L),
            feed.sortedFor(FeedSort.BOARD).map(FeedThread::threadId),
        )
    }

    @Test
    fun `board sort keeps providers apart even when board names collide`() {
        val feed = listOf(
            thread(provider = "lynxchan", board = "a", id = 1, created = 100),
            thread(provider = "vichan", board = "a", id = 2, created = 200),
            thread(provider = "lynxchan", board = "a", id = 3, created = 300),
        )

        assertEquals(
            listOf("lynxchan", "lynxchan", "vichan"),
            feed.sortedFor(FeedSort.BOARD).map(FeedThread::provider),
        )
    }

    @Test
    fun `board sort ignores activity so a bumped old thread stays put`() {
        val old = thread(board = "a", id = 1, created = 100, activity = 999)
        val new = thread(board = "a", id = 2, created = 200, activity = 200)

        assertEquals(
            listOf(2L, 1L),
            listOf(old, new).sortedFor(FeedSort.BOARD).map(FeedThread::threadId),
        )
    }

    @Test
    fun `board sort breaks creation ties by thread id so refreshes do not reshuffle`() {
        val feed = listOf(
            thread(board = "a", id = 1, created = 100),
            thread(board = "a", id = 3, created = 100),
            thread(board = "a", id = 2, created = 100),
        )

        assertEquals(
            listOf(3L, 2L, 1L),
            feed.sortedFor(FeedSort.BOARD).map(FeedThread::threadId),
        )
    }

    @Test
    fun `activity sort is a flat list ordered by last activity`() {
        val feed = listOf(
            thread(board = "a", id = 1, created = 100, activity = 100),
            thread(board = "b", id = 2, created = 50, activity = 300),
            thread(board = "a", id = 3, created = 200, activity = 200),
        )

        assertEquals(
            listOf(2L, 3L, 1L),
            feed.sortedFor(FeedSort.ACTIVITY).map(FeedThread::threadId),
        )
    }

    @Test
    fun `creation time defaults to last activity when a provider omits it`() {
        val thread = FeedThread(
            provider = "vichan",
            board = "a",
            threadId = 1,
            title = "t",
            lastActivityEpochMillis = 500,
        )

        assertEquals(500, thread.createdAtEpochMillis)
    }
}
