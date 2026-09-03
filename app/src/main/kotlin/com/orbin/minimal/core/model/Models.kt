package com.orbin.minimal.core.model

data class BoardRef(
    val provider: String,
    val board: String,
    val title: String = board,
)

data class MediaRef(
    val url: String,
    val thumbnailUrl: String? = null,
    val mimeType: String? = null,
)

data class FeedThread(
    val provider: String,
    val board: String,
    val threadId: Long,
    val title: String,
    val excerpt: String = "",
    val lastActivityEpochMillis: Long,
    // Thread creation time. Defaults to the last activity so a provider that
    // cannot supply it still orders sensibly instead of sinking to the bottom.
    val createdAtEpochMillis: Long = lastActivityEpochMillis,
    val media: MediaRef? = null,
)

data class ThreadPost(
    val id: Long,
    val author: String? = null,
    val body: String,
    val timestampEpochMillis: Long,
    val media: List<MediaRef> = emptyList(),
    val links: List<String> = emptyList(),
)

data class ThreadDetails(
    val provider: String,
    val board: String,
    val threadId: Long,
    val title: String,
    val posts: List<ThreadPost>,
)
