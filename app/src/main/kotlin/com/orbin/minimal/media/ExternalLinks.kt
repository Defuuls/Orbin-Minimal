package com.orbin.minimal.media

fun extractExternalLinks(text: String): List<String> =
    URL_PATTERN.findAll(text.replace("&amp;", "&"))
        .map { it.value.trimEnd('.', ',', ')', ']', '}', ';') }
        .filter { it.startsWith("https://", ignoreCase = true) }
        .distinct()
        .toList()

fun isSafeExternalLink(url: String): Boolean =
    url.startsWith("https://", ignoreCase = true)

private val URL_PATTERN = Regex("https?://[^\\s\\\"'<>]+", RegexOption.IGNORE_CASE)
