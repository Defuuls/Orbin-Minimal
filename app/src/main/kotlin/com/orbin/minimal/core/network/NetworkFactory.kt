package com.orbin.minimal.core.network

import android.content.Context
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

object NetworkFactory {
    private const val HTTP_CACHE_DIR = "http_json_cache"
    private const val HTTP_CACHE_BYTES = 5L * 1024L * 1024L

    fun sharedOkHttp(context: Context): OkHttpClient {
        val cacheDir = File(context.cacheDir, HTTP_CACHE_DIR)
        return OkHttpClient.Builder()
            .cache(Cache(cacheDir, HTTP_CACHE_BYTES))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(30, TimeUnit.SECONDS)
            // Network interceptor so rewritten Cache-Control is what the cache stores.
            .addNetworkInterceptor(JsonGetCacheInterceptor())
            .build()
    }
}
