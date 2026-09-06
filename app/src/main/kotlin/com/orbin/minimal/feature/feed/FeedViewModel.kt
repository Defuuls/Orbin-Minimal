package com.orbin.minimal.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.orbin.minimal.core.data.FeedRepository
import com.orbin.minimal.core.data.FeedSort
import com.orbin.minimal.core.data.ProviderFailure
import com.orbin.minimal.core.model.BoardRef
import com.orbin.minimal.core.model.FeedThread
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val warnings: List<ProviderFailure> = emptyList(),
    val feed: List<FeedThread> = emptyList(),
    val followed: List<BoardRef> = emptyList(),
    val sort: FeedSort = FeedSort.DEFAULT,
    val refreshKey: Int = 0,
)

class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(FeedUiState(followed = repository.followed()))
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun setSort(sort: FeedSort) {
        _state.update { it.copy(sort = sort) }
    }

    fun refresh() {
        val key = _state.value.refreshKey + 1
        _state.update {
            it.copy(
                refreshKey = key,
                loading = true,
                error = null,
                warnings = emptyList(),
                followed = repository.followed(),
            )
        }
        viewModelScope.launch {
            runCatching { repository.mergedFeedDetailed() }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            feed = result.value,
                            warnings = result.failures,
                            error = if (result.isTotalFailure) result.failureSummary() else null,
                            loading = false,
                            followed = repository.followed(),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            error = error.message ?: "Unable to load feed",
                            loading = false,
                            followed = repository.followed(),
                        )
                    }
                }
        }
    }

    companion object {
        fun factory(repository: FeedRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FeedViewModel(repository) as T
            }
    }
}
