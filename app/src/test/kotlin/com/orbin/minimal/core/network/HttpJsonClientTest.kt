package com.orbin.minimal.core.network

import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpJsonClientTest {
    @Test
    fun responseAtLimitIsAccepted() {
        val body = "1234".toResponseBody(JSON_MEDIA_TYPE)
        val client = HttpJsonClient(clientReturning(body), maxResponseBytes = 4)

        assertEquals("1234", runBlocking { client.get(TEST_URL) })
    }

    @Test
    fun oversizedUnknownLengthResponseIsRejected() {
        val client = HttpJsonClient(
            clientReturning(UnknownLengthBody("12345")),
            maxResponseBytes = 4,
        )

        val error = assertThrows(IllegalStateException::class.java) {
            runBlocking { client.get(TEST_URL) }
        }

        assertTrue(error.message.orEmpty().contains("exceeds 4 bytes"))
    }

    private fun clientReturning(body: ResponseBody): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(body)
                    .build()
            }
            .build()

    private class UnknownLengthBody(private val value: String) : ResponseBody() {
        override fun contentType(): MediaType = JSON_MEDIA_TYPE

        override fun contentLength(): Long = -1L

        override fun source(): BufferedSource = Buffer().writeUtf8(value)
    }

    private companion object {
        const val TEST_URL = "https://example.test/data"
        val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
