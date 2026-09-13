package com.orbin.minimal.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import java.util.concurrent.TimeUnit

/**
 * Thin JSON GET client. Network IO runs on [Dispatchers.IO]; callers must parse JSON / HTML off
 * the main thread (typically [Dispatchers.Default]).
 *
 * Requests carry a short max-age Cache-Control so OkHttp prefers the disk cache within
 * [cacheMaxAgeSeconds]; [JsonGetCacheInterceptor] ensures responses are stored with the same window.
 * Response bodies are capped at [maxResponseBytes] before materializing them as strings.
 */
class HttpJsonClient(
    private val client: OkHttpClient,
    private val userAgent: String = "Orbin-Minimal/1.0",
    private val cacheMaxAgeSeconds: Int = JsonGetCacheInterceptor.DEFAULT_MAX_AGE_SECONDS,
    private val maxResponseBytes: Long = DEFAULT_MAX_RESPONSE_BYTES,
) {
    init {
        require(maxResponseBytes > 0) { "maxResponseBytes must be positive" }
    }

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
            val body = response.body.readUtf8Limited(maxResponseBytes)
            if (!response.isSuccessful) {
                error("HTTP ${response.code} for $url: ${body.take(240)}")
            }
            body
        }
    }

    companion object {
        const val DEFAULT_MAX_RESPONSE_BYTES = 8L * 1024L * 1024L
    }
}

internal fun ResponseBody?.readUtf8Limited(maxBytes: Long): String {
    if (this == null) return ""

    val declaredLength = contentLength()
    if (declaredLength > maxBytes) {
        error("HTTP response body exceeds $maxBytes bytes")
    }

    val source = source()
    if (source.request(maxBytes + 1)) {
        error("HTTP response body exceeds $maxBytes bytes")
    }
    return source.readUtf8()
}
