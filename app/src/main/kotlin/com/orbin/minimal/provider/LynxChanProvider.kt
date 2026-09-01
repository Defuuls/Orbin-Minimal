package com.orbin.minimal.provider

import android.text.Html
import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import com.orbin.minimal.core.model.MediaRef
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.core.model.ThreadPost
import com.orbin.minimal.core.network.HttpJsonClient
import com.orbin.minimal.core.provider.ImageBoardProvider
import com.orbin.minimal.media.extractExternalLinks
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class LynxChanProvider(
    private val client: HttpJsonClient,
    override val id: String = "bbwchan",
    override val displayName: String = "BBW Chan",
    private val siteUrl: String = "https://bbw-chan.link",
) : ImageBoardProvider {
    override suspend fun boards(): List<BoardRef> {
        val root = JSONObject(client.get("$siteUrl/boards.js?json=1"))
        val boards = root.optJSONObject("data")?.optJSONArray("boards") ?: JSONArray()
        return buildList {
            for (index in 0 until boards.length()) {
                val item = boards.optJSONObject(index) ?: continue
                val board = item.optString("boardUri")
                if (board.isNotBlank() && !item.optBoolean("inactive")) {
                    add(BoardRef(id, board, item.optString("boardName", board)))
                }
            }
        }
    }

    override suspend fun catalog(board: String): List<FeedThread> {
        val threads = JSONArray(client.get("$siteUrl/$board/catalog.json"))
        return buildList {
            for (index in 0 until threads.length()) {
                val item = threads.optJSONObject(index) ?: continue
                add(
                    FeedThread(
                        provider = id,
                        board = board,
                        threadId = item.optLong("threadId"),
                        title = item.optString("subject").takeIf(String::isNotBlank)
                            ?: plainText(item.optString("markdown")).take(100),
                        excerpt = plainText(item.optString("markdown")),
                        lastActivityEpochMillis = parseInstant(item.optString("lastBump")),
                        media = item.optString("thumb").takeIf(String::isNotBlank)?.let {
                            MediaRef(
                                url = absolute(it),
                                thumbnailUrl = absolute(it),
                                mimeType = item.optString("mime").takeIf(String::isNotBlank),
                            )
                        },
                    ),
                )
            }
        }
    }

    override suspend fun thread(board: String, threadId: Long): ThreadDetails {
        val root = JSONObject(client.get("$siteUrl/$board/res/$threadId.json"))
        val posts = buildList {
            add(root.toPost())
            val replies = root.optJSONArray("posts") ?: JSONArray()
            for (index in 0 until replies.length()) {
                replies.optJSONObject(index)?.let { add(it.toPost()) }
            }
        }
        return ThreadDetails(
            provider = id,
            board = board,
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
                if (path.isBlank()) continue
                add(
                    MediaRef(
                        url = absolute(path),
                        thumbnailUrl = file.optString("thumb").takeIf(String::isNotBlank)?.let(::absolute),
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

    private fun absolute(path: String): String =
        if (path.startsWith("http://") || path.startsWith("https://")) path
        else "$siteUrl/${path.trimStart('/')}"

    private fun parseInstant(value: String): Long =
        runCatching { Instant.parse(value).toEpochMilli() }.getOrDefault(0L)

    @Suppress("DEPRECATION")
    private fun plainText(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}
