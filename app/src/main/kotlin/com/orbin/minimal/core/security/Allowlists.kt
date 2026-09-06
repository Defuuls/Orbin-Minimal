package com.orbin.minimal.core.security

import java.net.URI
import java.util.Locale

/** Board slug validation used on ingest, persistence reads, and navigation args. */
object BoardSlugs {
    private val PATTERN = Regex("^[a-zA-Z0-9_]+$")
    private const val MAX_LENGTH = 64

    fun isValid(board: String): Boolean =
        board.isNotEmpty() && board.length <= MAX_LENGTH && PATTERN.matches(board)

    fun sanitizeOrNull(board: String): String? = board.takeIf(::isValid)
}

/**
 * Hosts permitted for Coil, ExoPlayer, and DownloadManager. Provider API hosts are included so
 * absolute media URLs that land on the same origin as catalogs remain loadable.
 */
object MediaHosts {
    private val ALLOWED_HOSTS =
        setOf(
            "i.4cdn.org",
            "a.4cdn.org",
            "bbw-chan.link",
            "www.bbw-chan.link",
        )

    fun isAllowedUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase(Locale.US) ?: return false
        return ALLOWED_HOSTS.any { allowed -> host == allowed || host.endsWith(".$allowed") }
    }

    fun filterUrl(url: String?): String? = url?.takeIf(::isAllowedUrl)
}

/**
 * LynxChan media path sanitizer. Rejects protocol-relative URLs, traversal, and non-http(s)
 * schemes so absolute() cannot open arbitrary off-site locations.
 */
object MediaPaths {
    fun toSafeMediaLocation(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty() || value.hasUnsafeCharacters()) return null

        if (value.startsWith('/')) {
            val isSafePath = !value.startsWith("//") && value.split('/').none { it == ".." }
            return value.takeIf { isSafePath }
        }

        if (value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            val uri = runCatching { URI(value) }.getOrNull() ?: return null
            val scheme = uri.scheme?.lowercase(Locale.US)
            if (scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) return null
            // Absolute URLs are only kept when the host is allowlisted.
            return value.takeIf { MediaHosts.isAllowedUrl(it) }
        }

        // Relative path without leading slash (e.g. ".media/file.jpg")
        if (value.contains("://") || value.startsWith("//")) return null
        if (value.split('/').any { it == ".." }) return null
        return value
    }

    fun resolveAbsolute(siteUrl: String, location: String): String {
        if (location.startsWith("http://", ignoreCase = true) ||
            location.startsWith("https://", ignoreCase = true)
        ) {
            return location
        }
        val base = siteUrl.trimEnd('/')
        return if (location.startsWith('/')) "$base$location" else "$base/${location.trimStart('/')}"
    }

    private fun String.hasUnsafeCharacters(): Boolean =
        any { it.isISOControl() || it.isWhitespace() } || contains('\\')
}
