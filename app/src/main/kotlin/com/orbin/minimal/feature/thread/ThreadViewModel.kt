package com.orbin.minimal.feature.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.orbin.minimal.core.data.ThreadRepository
import com.orbin.minimal.core.model.ThreadDetails
import com.orbin.minimal.core.security.BoardSlugs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ThreadUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val thread: ThreadDetails? = null,
)

class ThreadViewModel(
    private val repository: ThreadRepository,
    private val provider: String,
    private val board: String,
    private val threadId: Long,
) : ViewModel() {
    private val _state = MutableStateFlow(ThreadUiState())
    val state: StateFlow<ThreadUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        val safeBoard = BoardSlugs.sanitizeOrNull(board)
        if (safeBoard == null) {
            _state.update {
                it.copy(loading = false, error = "Invalid board slug", thread = null)
            }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.load(provider, safeBoard, threadId) }
                .onSuccess { details ->
                    _state.update { it.copy(thread = details, loading = false, error = null) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Unable to load thread",
                            loading = false,
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(
            repository: ThreadRepository,
            provider: String,
            board: String,
            threadId: Long,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ThreadViewModel(repository, provider, board, threadId) as T
            }
    }
}
