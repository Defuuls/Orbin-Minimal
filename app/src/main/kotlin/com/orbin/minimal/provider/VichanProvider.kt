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
import com.orbin.minimal.media.extractExternalLinks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class VichanProvider(
    private val client: HttpJsonClient,
    override val id: String = "fourchan",
    override val displayName: String = "4chan",
    private val apiBaseUrl: String = "https://a.4cdn.org",
    private val mediaBaseUrl: String = "https://i.4cdn.org",
) : ImageBoardProvider {
    override suspend fun boards(): List<BoardRef> =
        withContext(Dispatchers.Default) {
            val array = JSONObject(client.get("$apiBaseUrl/boards.json")).optJSONArray("boards") ?: JSONArray()
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val board = BoardSlugs.sanitizeOrNull(item.optString("board")) ?: continue
                    add(BoardRef(id, board, item.optString("title", board)))
                }
            }
        }

    override suspend fun catalog(board: String): List<FeedThread> =
        withContext(Dispatchers.Default) {
            val safeBoard = BoardSlugs.sanitizeOrNull(board) ?: return@withContext emptyList()
            val pages = JSONArray(client.get("$apiBaseUrl/$safeBoard/catalog.json"))
            buildList {
                for (pageIndex in 0 until pages.length()) {
                    val threads = pages.optJSONObject(pageIndex)?.optJSONArray("threads") ?: continue
                    for (threadIndex in 0 until threads.length()) {
                        val post = threads.optJSONObject(threadIndex) ?: continue
                        add(post.toFeedThread(safeBoard))
                    }
                }
            }
        }

    override suspend fun thread(board: String, threadId: Long): ThreadDetails =
        withContext(Dispatchers.Default) {
            val safeBoard = BoardSlugs.sanitizeOrNull(board)
                ?: error("Invalid board slug")
            val posts = JSONObject(client.get("$apiBaseUrl/$safeBoard/thread/$threadId.json"))
                .optJSONArray("posts") ?: JSONArray()
            val mapped = buildList {
                for (index in 0 until posts.length()) {
                    posts.optJSONObject(index)?.let { add(it.toPost(safeBoard)) }
                }
            }
            val op = posts.optJSONObject(0)
            ThreadDetails(
                provider = id,
                board = safeBoard,
                threadId = threadId,
                title = op?.optString("sub")?.takeIf(String::isNotBlank) ?: "Thread $threadId",
                posts = mapped,
            )
        }

    private fun JSONObject.toFeedThread(board: String): FeedThread {
        val threadId = optLong("no")
        val created = optLong("time")
        val modified = optLong("last_modified").takeIf { it > 0 } ?: created
        return FeedThread(
            provider = id,
            board = board,
            threadId = threadId,
            title = optString("sub").takeIf(String::isNotBlank) ?: plainText(optString("com")).take(100),
            excerpt = plainText(optString("com")),
            lastActivityEpochMillis = modified * 1_000L,
            createdAtEpochMillis = created.takeIf { it > 0 }?.times(1_000L) ?: (modified * 1_000L),
            media = media(board),
        )
    }

    private fun JSONObject.toPost(board: String): ThreadPost {
        val rawComment = optString("com")
        return ThreadPost(
            id = optLong("no"),
            author = optString("name").takeIf(String::isNotBlank),
            body = plainText(rawComment),
            timestampEpochMillis = optLong("time") * 1_000L,
            media = listOfNotNull(media(board)),
            links = extractExternalLinks(rawComment),
        )
    }

    private fun JSONObject.media(board: String): MediaRef? {
        val tim = optString("tim")
        val ext = optString("ext")
        if (tim.isBlank() || ext.isBlank()) return null
        val url = MediaHosts.filterUrl("$mediaBaseUrl/$board/$tim$ext") ?: return null
        val thumb = MediaHosts.filterUrl("$mediaBaseUrl/$board/${tim}s.jpg")
        return MediaRef(
            url = url,
            thumbnailUrl = thumb,
        )
    }

    @Suppress("DEPRECATION")
    private fun plainText(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}
