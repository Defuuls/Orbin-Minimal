package com.orbin.minimal.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Thin JSON GET client. Network IO runs on [Dispatchers.IO]; callers must parse JSON / HTML off
 * the main thread (typically [Dispatchers.Default]).
 */
class HttpJsonClient(
    private val client: OkHttpClient,
    private val userAgent: String = "Orbin-Minimal/1.0",
    private val cacheMaxAgeSeconds: Int = 60,
) {
    suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        val request =
            Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .cacheControl(
                    CacheControl.Builder()
                        .maxAge(cacheMaxAgeSeconds, TimeUnit.SECONDS)
                        .build(),
                )
                .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("HTTP ${response.code} for $url: ${body.take(240)}")
            }
            body
        }
    }
}
