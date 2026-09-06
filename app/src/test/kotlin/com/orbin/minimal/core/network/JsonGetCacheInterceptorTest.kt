package com.orbin.minimal.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonGetCacheInterceptorTest {
    @Test
    fun `shouldRewrite only for successful json GETs`() {
        assertTrue(
            JsonGetCacheInterceptor.shouldRewrite("GET", "application/json", successful = true),
        )
        assertTrue(
            JsonGetCacheInterceptor.shouldRewrite("GET", "application/json, text/plain", successful = true),
        )
        assertFalse(
            JsonGetCacheInterceptor.shouldRewrite("POST", "application/json", successful = true),
        )
        assertFalse(
            JsonGetCacheInterceptor.shouldRewrite("GET", "image/*", successful = true),
        )
        assertFalse(
            JsonGetCacheInterceptor.shouldRewrite("GET", "application/json", successful = false),
        )
        assertFalse(
            JsonGetCacheInterceptor.shouldRewrite("GET", null, successful = true),
        )
    }

    @Test
    fun `rewrites json GET response cache headers for OkHttp storage`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Cache-Control", "no-store")
                    .setHeader("Pragma", "no-cache")
                    .setBody("""{"ok":true}"""),
            )
            val client =
                OkHttpClient.Builder()
                    .addNetworkInterceptor(JsonGetCacheInterceptor(maxAgeSeconds = 60))
                    .build()
            val response =
                client.newCall(
                    Request.Builder()
                        .url(server.url("/catalog.json"))
                        .header("Accept", "application/json")
                        .build(),
                ).execute()

            assertTrue(response.isSuccessful)
            assertEquals("public, max-age=60", response.header("Cache-Control"))
            assertNull(response.header("Pragma"))
            response.close()
        }
    }

    @Test
    fun `does not rewrite image Accept GETs`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse()
                    .setResponseCode(200)
                    .setHeader("Cache-Control", "max-age=86400")
                    .setBody("fake"),
            )
            val client =
                OkHttpClient.Builder()
                    .addNetworkInterceptor(JsonGetCacheInterceptor())
                    .build()
            val response =
                client.newCall(
                    Request.Builder()
                        .url(server.url("/thumb.jpg"))
                        .header("Accept", "image/*")
                        .build(),
                ).execute()

            assertEquals("max-age=86400", response.header("Cache-Control"))
            response.close()
        }
    }
}
