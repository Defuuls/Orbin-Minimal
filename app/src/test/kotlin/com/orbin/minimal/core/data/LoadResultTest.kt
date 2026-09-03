package com.orbin.minimal.core.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadResultTest {
    private val failure = ProviderFailure(
        provider = "test",
        operation = "load catalog",
        message = "boom",
    )

    @Test
    fun `partial result reports partial failure`() {
        val result = LoadResult(value = listOf("thread"), failures = listOf(failure))

        assertTrue(result.isPartial)
        assertFalse(result.isTotalFailure)
    }

    @Test
    fun `empty failed result reports total failure`() {
        val result = LoadResult(value = emptyList<String>(), failures = listOf(failure))

        assertFalse(result.isPartial)
        assertTrue(result.isTotalFailure)
    }

    @Test
    fun `successful empty result is not a failure`() {
        val result = LoadResult(value = emptyList<String>())

        assertFalse(result.isPartial)
        assertFalse(result.isTotalFailure)
    }

    @Test
    fun `failure summary includes provider operation and message`() {
        val summary = LoadResult(value = emptyList<String>(), failures = listOf(failure)).failureSummary()

        assertTrue(summary.contains("test"))
        assertTrue(summary.contains("load catalog"))
        assertTrue(summary.contains("boom"))
    }
}
