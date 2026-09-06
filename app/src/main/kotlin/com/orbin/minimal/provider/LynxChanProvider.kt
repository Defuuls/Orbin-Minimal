package com.orbin.minimal.provider

import android.text.Html
import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.MediaRef
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.core.model.ThreadPost
import com.orbin.minimal.core.network.HttpJsonClient
import com.orbin.minimal.core.provider.ImageBoardProvider
import com.orbin.minimal.core.security.BoardSlugs
import com.orbin.minimal.core.security.MediaHosts
import com.orbin.minimal.core.security.MediaPaths
import com.orbin.minimal.media.extractExternalLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class LynxChanProvider(
    private val client: HttpJsonClient,
    override val id: String = "bbwchan",
    override val displayName: String = "BBW Chan",
    private val siteUrl: String = "https://bbw-chan.link",
) : ImageBoardProvider {
    override suspend fun boards(): List<BoardRef> =
        withContext(Dispatchers.Default) {
            val root = JSONObject(client.get("$siteUrl/boards.js?json=1"))
            val boards = root.optJSONObject("data")?.optJSONArray("boards") ?: JSONArray()
            buildList {
                for (index in 0 until boards.length()) {
                    val item = boards.optJSONObject(index) ?: continue
                    val board = BoardSlugs.sanitizeOrNull(item.optString("boardUri")) ?: continue
                    if (!item.optBoolean("inactive")) {
                        add(BoardRef(id, board, item.optString("boardName", board)))
                    }
                }
            }
        }

    override suspend fun catalog(board: String): List<FeedThread> =
        withContext(Dispatchers.Default) {
            val safeBoard = BoardSlugs.sanitizeOrNull(board) ?: return@withContext emptyList()
            val threads = JSONArray(client.get("$siteUrl/$safeBoard/catalog.json"))
            buildList {
                for (index in 0 until threads.length()) {
                    val item = threads.optJSONObject(index) ?: continue
                    add(
                        FeedThread(
                            provider = id,
                            board = safeBoard,
                            threadId = item.optLong("threadId"),
                            title = item.optString("subject").takeIf(String::isNotBlank)
                                ?: plainText(item.optString("markdown")).take(100),
                            excerpt = plainText(item.optString("markdown")),
                            lastActivityEpochMillis = parseInstant(item.optString("lastBump")),
                            createdAtEpochMillis = parseInstant(item.optString("creation"))
                                .takeIf { it > 0 }
                                ?: parseInstant(item.optString("lastBump")),
                            media = item.optString("thumb").takeIf(String::isNotBlank)?.let { thumb ->
                                absoluteMedia(thumb)?.let { url ->
                                    MediaRef(
                                        url = url,
                                        thumbnailUrl = url,
                                        mimeType = item.optString("mime").takeIf(String::isNotBlank),
                                    )
                                }
                            },
                        ),
                    )
                }
            }
        }

    override suspend fun thread(board: String, threadId: Long): ThreadDetails =
        withContext(Dispatchers.Default) {
            val safeBoard = BoardSlugs.sanitizeOrNull(board)
                ?: error("Invalid board slug")
            val root = JSONObject(client.get("$siteUrl/$safeBoard/res/$threadId.json"))
            val posts = buildList {
                add(root.toPost())
                val replies = root.optJSONArray("posts") ?: JSONArray()
                for (index in 0 until replies.length()) {
                    replies.optJSONObject(index)?.let { add(it.toPost()) }
                }
            }
            ThreadDetails(
                provider = id,
                board = safeBoard,
                threadId = threadId,
                title = root.optString("subject").takeIf(String::isNotBlank) ?: "Thread $threadId",
                posts = posts,
            )
        }

    private fun JSONObject.toPost(): ThreadPost {
        val rawBody = optString("markdown")
        val files = optJSONArray("files") ?: JSONArray()
        val media = buildList {
            for (index in 0 until files.length()) {
                val file = files.optJSONObject(index) ?: continue
                val path = file.optString("path")
                val url = absoluteMedia(path) ?: continue
                add(
                    MediaRef(
                        url = url,
                        thumbnailUrl = file.optString("thumb").takeIf(String::isNotBlank)
                            ?.let(::absoluteMedia),
                        mimeType = file.optString("mime").takeIf(String::isNotBlank),
                    ),
                )
            }
        }
        return ThreadPost(
            id = optLong("postId").takeIf { it > 0 } ?: optLong("threadId"),
            author = optString("name").takeIf(String::isNotBlank),
            body = plainText(rawBody),
            timestampEpochMillis = parseInstant(optString("creation")),
            media = media,
            links = extractExternalLinks(rawBody),
        )
    }

    private fun absoluteMedia(path: String): String? {
        val safe = MediaPaths.toSafeMediaLocation(path) ?: return null
        val absolute = MediaPaths.resolveAbsolute(siteUrl, safe)
        return MediaHosts.filterUrl(absolute)
    }

    private fun parseInstant(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)

    @Suppress("DEPRECATION")
    private fun plainText(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}
