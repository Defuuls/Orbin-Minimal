package com.orbin.minimal.media

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.orbin.minimal.core.model.MediaRef
import com.orbin.minimal.core.model.ThreadDetails

class ThreadMediaSync(private val context: Context) {
    private val downloadManager: DownloadManager
        get() = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    /** Returns the number of media items successfully queued with DownloadManager. */
    fun sync(thread: ThreadDetails): Int {
        val media = thread.posts.flatMap { it.media }.distinctBy { it.url }
        return media.mapIndexed { index, item ->
            runCatching {
                enqueue(
                    media = item,
                    board = thread.board,
                    threadId = thread.threadId,
                    threadTitle = thread.title,
                    fallbackIndex = index + 1,
                )
            }.getOrDefault(SKIPPED_ID)
        }.count { it != SKIPPED_ID }
    }

    fun enqueue(
        media: MediaRef,
        board: String,
        threadId: Long,
        threadTitle: String,
        fallbackIndex: Int = 1,
    ): Long {
        val uri = Uri.parse(media.url)
        if (!uri.scheme.equals("https", ignoreCase = true)) return SKIPPED_ID

        val fileName = safeFileName(uri.lastPathSegment, fallbackIndex)
        val relativeDir = buildThreadRelativeDir(board, threadId, threadTitle)
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription("Orbin Minimal thread media")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "Orbin Minimal/$relativeDir$fileName",
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        return downloadManager.enqueue(request)
    }

    internal fun buildThreadRelativeDir(
        board: String,
        threadId: Long,
        threadTitle: String,
    ): String {
        val safeBoard = sanitizePathSegment(board).ifBlank { "board" }
        val safeTitle = sanitizePathSegment(threadTitle)
        val threadFolder = if (safeTitle.isBlank()) "$threadId" else "$threadId - $safeTitle"
        return "$safeBoard/$threadFolder/"
    }

    private fun safeFileName(raw: String?, fallbackIndex: Int): String {
        val cleaned = raw
            .orEmpty()
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .filterNot { it.isISOControl() }
            .replace(UNSAFE_PATH_CHARS, "_")
            .replace("..", "_")
            .trim(' ', '.')
            .takeLast(MAX_FILENAME_LENGTH)
        return cleaned.ifBlank { "media-$fallbackIndex" }
    }

    private fun sanitizePathSegment(raw: String): String =
        raw
            .filterNot { it.isISOControl() }
            .replace(UNSAFE_PATH_CHARS, "_")
            .replace("..", "_")
            .trim(' ', '.')
            .take(MAX_PATH_SEGMENT_LENGTH)

    private companion object {
        const val SKIPPED_ID = -1L
        const val MAX_FILENAME_LENGTH = 200
        const val MAX_PATH_SEGMENT_LENGTH = 80
        val UNSAFE_PATH_CHARS = Regex("""[/\\:*?\"<>|]""")
    }
}
