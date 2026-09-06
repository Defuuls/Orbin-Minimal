package com.orbin.minimal.core.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Ensures catalog/thread JSON GETs can land in OkHttp's disk cache with a short freshness window
 * (Orbin-style max-age=60), even when origin omits Cache-Control or sends aggressive no-cache.
 *
 * Only rewrites successful GETs that asked for JSON ([ACCEPT_JSON]), so Coil image traffic sharing
 * the same client keeps origin/CDN cache semantics.
 */
class JsonGetCacheInterceptor(
    private val maxAgeSeconds: Int = DEFAULT_MAX_AGE_SECONDS,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!shouldRewrite(request.method, request.header(HEADER_ACCEPT), response.isSuccessful)) {
            return response
        }
        return response.newBuilder()
            .header(HEADER_CACHE_CONTROL, "public, max-age=$maxAgeSeconds")
            .removeHeader(HEADER_PRAGMA)
            .removeHeader(HEADER_EXPIRES)
            .build()
    }

    companion object {
        const val DEFAULT_MAX_AGE_SECONDS = 60
        const val HEADER_ACCEPT = "Accept"
        const val HEADER_CACHE_CONTROL = "Cache-Control"
        const val HEADER_PRAGMA = "Pragma"
        const val HEADER_EXPIRES = "Expires"
        const val ACCEPT_JSON = "application/json"

        fun shouldRewrite(method: String, accept: String?, successful: Boolean): Boolean =
            successful &&
                method.equals("GET", ignoreCase = true) &&
                accept?.contains(ACCEPT_JSON, ignoreCase = true) == true
    }
}
