package com.dhs0319.bills.feature.history.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.history.HistoryRepository
import com.dhs0319.bills.core.model.HistoryItem
import com.dhs0319.bills.core.model.HistorySearchPage
import com.dhs0319.bills.core.model.HistoryTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorySearchUiState(
    val tab: HistoryTab = HistoryTab.ALL,
    val input: String = "",
    val query: String = "",
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null
) {
    val canLoadMore: Boolean
        get() = hasMore && !isLoading && !isLoadingMore
}

@HiltViewModel
class HistorySearchViewModel internal constructor(
    savedStateHandle: SavedStateHandle,
    private val searchHistory: suspend (String, HistoryTab, Int) -> HistorySearchPage
) : ViewModel() {

    @Inject constructor(
        savedStateHandle: SavedStateHandle,
        repo: HistoryRepository
    ) : this(savedStateHandle, repo::search)

    private val _uiState = MutableStateFlow(
        HistorySearchUiState(
            tab = HistoryTab.fromBusiness(
                savedStateHandle.get<String>(TAB_ARG).orEmpty()
            )
        )
    )
    val uiState: StateFlow<HistorySearchUiState> = _uiState.asStateFlow()

    private var nextPage = 1
    private var searchJob: Job? = null

    fun updateKeyword(value: String) {
        if (value.isBlank()) {
            clearSearch(input = value)
            return
        }
        _uiState.update { it.copy(input = value) }
    }

    private fun clearSearch(input: String = "") {
        searchJob?.cancel()
        searchJob = null
        nextPage = 1
        _uiState.update { HistorySearchUiState(tab = it.tab, input = input) }
    }

    fun submitSearch() {
        val state = _uiState.value
        val keyword = state.input.trim()
        if (keyword.isBlank()) {
            clearSearch()
            return
        }
        if (state.isLoading || state.isLoadingMore) return
        nextPage = 1
        _uiState.update {
            it.copy(
                input = keyword,
                query = keyword,
                items = emptyList(),
                isLoading = true,
                isLoadingMore = false,
                hasMore = false,
                errorMessage = null
            )
        }
        search(reset = true)
    }

    fun loadMore() {
        if (!_uiState.value.canLoadMore) return
        _uiState.update {
            it.copy(
                isLoadingMore = true,
                errorMessage = null
            )
        }
        search(reset = false)
    }

    private fun search(reset: Boolean) {
        val state = _uiState.value
        val pageNo = if (reset) 1 else nextPage
        searchJob = viewModelScope.launch {
            val result = runCatching {
                searchHistory(state.query, state.tab, pageNo)
            }
            currentCoroutineContext().ensureActive()
            result.onSuccess { page ->
                nextPage = pageNo + 1
                _uiState.update {
                    it.copy(
                        items = if (reset) page.items else it.items + page.items,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = page.hasMore,
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = "搜索历史记录失败"
                    )
                }
            }
        }
    }

    private companion object {
        const val TAB_ARG = "tab"
    }
}
