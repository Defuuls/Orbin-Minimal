package com.orbin.minimal.core.data

import android.content.Context
import com.orbin.minimal.core.model.BoardRef
import org.json.JSONArray
import org.json.JSONObject

class FollowedBoardStore(context: Context) {
    private val preferences = context.getSharedPreferences("followed_boards", Context.MODE_PRIVATE)

    fun all(): List<BoardRef> {
        val raw = preferences.getString(KEY_BOARDS, "[]") ?: "[]"
        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val provider = item.optString("provider")
                val board = item.optString("board")
                if (provider.isNotBlank() && board.isNotBlank()) {
                    add(BoardRef(provider, board, item.optString("title", board)))
                }
            }
        }
    }

    fun isFollowed(board: BoardRef): Boolean =
        all().any { it.provider == board.provider && it.board == board.board }

    fun toggle(board: BoardRef): Boolean {
        val current = all().toMutableList()
        val index = current.indexOfFirst { it.provider == board.provider && it.board == board.board }
        val followed = if (index >= 0) {
            current.removeAt(index)
            false
        } else {
            current.add(board)
            true
        }
        save(current)
        return followed
    }

    private fun save(boards: List<BoardRef>) {
        val array = JSONArray()
        boards.forEach { board ->
            array.put(
                JSONObject()
                    .put("provider", board.provider)
                    .put("board", board.board)
                    .put("title", board.title),
            )
        }
        preferences.edit().putString(KEY_BOARDS, array.toString()).apply()
    }

    private companion object {
        const val KEY_BOARDS = "boards"
    }
}
