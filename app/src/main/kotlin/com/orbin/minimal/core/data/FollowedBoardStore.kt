package com.orbin.minimal.core.data

import android.content.Context
import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.security.BoardSlugs
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persists followed boards. Keeps an in-memory snapshot so [all] / [isFollowed] do not
 * re-read and re-parse SharedPreferences JSON on every call; the snapshot is replaced on write.
 */
class FollowedBoardStore internal constructor(
    private val readRaw: () -> String,
    private val writeRaw: (String) -> Unit,
) {
    constructor(context: Context) : this(
        readRaw = {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_BOARDS, "[]") ?: "[]"
        },
        writeRaw = { value ->
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BOARDS, value)
                .apply()
        },
    )

    @Volatile
    private var cached: List<BoardRef>? = null

    fun all(): List<BoardRef> {
        cached?.let { return it }
        val loaded = parseBoards(readRaw())
        cached = loaded
        return loaded
    }

    fun isFollowed(board: BoardRef): Boolean =
        all().any { it.provider == board.provider && it.board == board.board }

    fun toggle(board: BoardRef): Boolean {
        val safeBoard = BoardSlugs.sanitizeOrNull(board.board) ?: return false
        val normalized = board.copy(board = safeBoard)
        val current = all().toMutableList()
        val index = current.indexOfFirst { it.provider == normalized.provider && it.board == normalized.board }
        val followed = if (index >= 0) {
            current.removeAt(index)
            false
        } else {
            current.add(normalized)
            true
        }
        save(current)
        return followed
    }

    private fun save(boards: List<BoardRef>) {
        val serialized = serializeBoards(boards)
        writeRaw(serialized)
        cached = parseBoards(serialized)
    }

    companion object {
        private const val PREFS_NAME = "followed_boards"
        private const val KEY_BOARDS = "boards"

        fun parseBoards(raw: String): List<BoardRef> {
            val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
            return buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val provider = item.optString("provider")
                    val board = BoardSlugs.sanitizeOrNull(item.optString("board")) ?: continue
                    if (provider.isNotBlank()) {
                        add(BoardRef(provider, board, item.optString("title", board)))
                    }
                }
            }
        }

        fun serializeBoards(boards: List<BoardRef>): String {
            val array = JSONArray()
            boards.forEach { board ->
                val safeBoard = BoardSlugs.sanitizeOrNull(board.board) ?: return@forEach
                array.put(
                    JSONObject()
                        .put("provider", board.provider)
                        .put("board", safeBoard)
                        .put("title", board.title),
                )
            }
            return array.toString()
        }
    }
}
