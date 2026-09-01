package com.orbin.minimal.media

fun extractExternalLinks(text: String): List<String> =
    URL_PATTERN.findAll(text.replace("&amp;", "&"))
        .map { it.value.trimEnd('.', ',', ')', ']', '}', ';') }
        .filter { it.startsWith("https://", ignoreCase = true) || it.startsWith("http://", ignoreCase = true) }
        .distinct()
        .toList()

private val URL_PATTERN = Regex("https?://[^\\s\\\"'<>]+", RegexOption.IGNORE_CASE)
