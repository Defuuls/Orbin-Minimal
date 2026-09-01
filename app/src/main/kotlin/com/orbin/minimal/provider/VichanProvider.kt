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

class VichanProvider(
    private val client: HttpJsonClient,
    override val id: String = "fourchan",
    override val displayName: String = "4chan",
    private val apiBaseUrl: String = "https://a.4cdn.org",
    private val mediaBaseUrl: String = "https://i.4cdn.org",
) : ImageBoardProvider {
    override suspend fun boards(): List<BoardRef> {
        val array = JSONObject(client.get("$apiBaseUrl/boards.json")).optJSONArray("boards") ?: JSONArray()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val board = item.optString("board")
                if (board.isNotBlank()) add(BoardRef(id, board, item.optString("title", board)))
            }
        }
    }

    override suspend fun catalog(board: String): List<FeedThread> {
        val pages = JSONArray(client.get("$apiBaseUrl/$board/catalog.json"))
        return buildList {
            for (pageIndex in 0 until pages.length()) {
                val threads = pages.optJSONObject(pageIndex)?.optJSONArray("threads") ?: continue
                for (threadIndex in 0 until threads.length()) {
                    val post = threads.optJSONObject(threadIndex) ?: continue
                    add(post.toFeedThread(board))
                }
            }
        }
    }

    override suspend fun thread(board: String, threadId: Long): ThreadDetails {
        val posts = JSONObject(client.get("$apiBaseUrl/$board/thread/$threadId.json"))
            .optJSONArray("posts") ?: JSONArray()
        val mapped = buildList {
            for (index in 0 until posts.length()) {
                posts.optJSONObject(index)?.let { add(it.toPost(board)) }
            }
        }
        val op = posts.optJSONObject(0)
        return ThreadDetails(
            provider = id,
            board = board,
            threadId = threadId,
            title = op?.optString("sub")?.takeIf(String::isNotBlank) ?: "Thread $threadId",
            posts = mapped,
        )
    }

    private fun JSONObject.toFeedThread(board: String): FeedThread {
        val threadId = optLong("no")
        val modified = optLong("last_modified").takeIf { it > 0 } ?: optLong("time")
        return FeedThread(
            provider = id,
            board = board,
            threadId = threadId,
            title = optString("sub").takeIf(String::isNotBlank) ?: plainText(optString("com")).take(100),
            excerpt = plainText(optString("com")),
            lastActivityEpochMillis = modified * 1_000L,
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
        return MediaRef(
            url = "$mediaBaseUrl/$board/$tim$ext",
            thumbnailUrl = "$mediaBaseUrl/$board/${tim}s.jpg",
        )
    }

    @Suppress("DEPRECATION")
    private fun plainText(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
}
