package com.dhs0319.bills.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.history.HistoryRepository
import com.dhs0319.bills.core.model.HistoryCursor
import com.dhs0319.bills.core.model.HistoryItem
import com.dhs0319.bills.core.model.HistoryTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val tab: HistoryTab = HistoryTab.ALL,
    val items: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
    val errorOnLoadMore: Boolean = false,
    val isLoggedIn: Boolean = false
) {
    val canLoadMore: Boolean
        get() = isLoggedIn && hasMore && !isLoading && !isRefreshing && !isLoadingMore
}

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo: HistoryRepository,
    private val authRepo: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        HistoryUiState(isLoggedIn = authRepo.currentMidFlow.value > 0L)
    )
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val cache = mutableMapOf<HistoryTab, HistoryCache>()
    private val deletingItems = mutableSetOf<Pair<String, Long>>()
    private val deletedItems = mutableMapOf<Pair<String, Long>, Long>()
    private var cursor = HistoryCursor()
    private var loadJob: Job? = null
    private var authVersion = 0L

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow.collect { mid ->
                authVersion += 1L
                loadJob?.cancel()
                cache.clear()
                deletingItems.clear()
                deletedItems.clear()
                cursor = HistoryCursor()
                _actionMessage.value = null
                _uiState.value = HistoryUiState(tab = _uiState.value.tab, isLoggedIn = mid > 0L)
                if (mid > 0L) refresh()
            }
        }
    }

    fun selectTab(tab: HistoryTab) {
        val state = _uiState.value
        if (state.tab == tab) return
        if (!state.isLoggedIn) {
            _uiState.update { it.copy(tab = tab) }
            return
        }
        loadJob?.cancel()
        val cached = cache[tab]
        if (cached != null) {
            cursor = cached.cursor
            _uiState.value = HistoryUiState(
                tab = tab,
                items = cached.items,
                hasMore = cached.hasMore,
                isLoggedIn = true
            )
            return
        }
        cursor = HistoryCursor()
        _uiState.update {
            it.copy(
                tab = tab,
                items = emptyList(),
                isLoading = true,
                isRefreshing = false,
                isLoadingMore = false,
                hasMore = false,
                errorMessage = null,
                errorOnLoadMore = false
            )
        }
        loadJob = viewModelScope.launch {
            load(tab = tab, reset = true, refreshing = false)
        }
    }

    fun previewState(tab: HistoryTab): HistoryUiState {
        val state = _uiState.value
        if (state.tab == tab) return state
        if (!state.isLoggedIn) return HistoryUiState(tab = tab)
        val cached = cache[tab] ?: return HistoryUiState(tab = tab, isLoading = true, isLoggedIn = true)
        return HistoryUiState(tab = tab, items = cached.items, hasMore = cached.hasMore, isLoggedIn = true)
    }

    fun observeTab(tab: HistoryTab): Flow<HistoryUiState> {
        return uiState.map { state ->
            if (state.tab == tab) state else previewState(tab)
        }.distinctUntilChanged()
    }

    fun refresh() {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isLoading || state.isRefreshing || state.isLoadingMore) return
        val hasItems = state.items.isNotEmpty()
        val tab = state.tab
        cursor = HistoryCursor()
        _uiState.update {
            it.copy(
                items = if (hasItems) it.items else emptyList(),
                isLoading = !hasItems,
                isRefreshing = hasItems,
                isLoadingMore = false,
                hasMore = if (hasItems) it.hasMore else false,
                errorMessage = null,
                errorOnLoadMore = false
            )
        }
        loadJob = viewModelScope.launch {
            load(tab = tab, reset = true, refreshing = hasItems)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.canLoadMore) return
        _uiState.update {
            it.copy(
                isLoadingMore = true,
                errorMessage = null,
                errorOnLoadMore = false
            )
        }
        loadJob = viewModelScope.launch {
            load(tab = state.tab, reset = false, refreshing = false)
        }
    }

    fun deleteItem(item: HistoryItem) {
        if (!_uiState.value.isLoggedIn) return
        val identity = item.type to item.historyId
        if (!deletingItems.add(identity)) return
        val version = authVersion
        viewModelScope.launch {
            try {
                repo.delete(item)
                if (version != authVersion) return@launch
                deletedItems[identity] = maxOf(
                    deletedItems[identity] ?: Long.MIN_VALUE,
                    item.viewedAtSec
                )
                cache.replaceAll { _, cached ->
                    cached.copy(items = withoutDeletedItems(cached.items))
                }
                _uiState.update { it.copy(items = withoutDeletedItems(it.items)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version != authVersion) return@launch
                Logger.e(TAG, e) { "删除历史记录失败" }
                _actionMessage.value = e.message?.takeIf(String::isNotBlank) ?: "删除历史记录失败"
            } finally {
                if (version == authVersion) deletingItems.remove(identity)
            }
        }
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    private fun withoutDeletedItems(items: List<HistoryItem>): List<HistoryItem> {
        return items.filter { item ->
            val deletedAt = deletedItems[item.type to item.historyId]
            deletedAt == null || item.viewedAtSec > deletedAt
        }
    }

    private suspend fun load(
        tab: HistoryTab,
        reset: Boolean,
        refreshing: Boolean
    ) {
        try {
            val page = repo.fetchPage(tab, if (reset) HistoryCursor() else cursor)
            currentCoroutineContext().ensureActive()
            val state = _uiState.value
            if (state.tab != tab) return
            val items = withoutDeletedItems(
                when {
                    reset -> page.items
                    page.items.isEmpty() -> state.items
                    else -> state.items + page.items
                }
            )
            cursor = page.cursor
            cache[tab] = HistoryCache(
                items = items,
                cursor = page.cursor,
                hasMore = page.hasMore
            )
            _uiState.update {
                if (it.tab != tab) return@update it
                it.copy(
                    items = items,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    hasMore = page.hasMore,
                    errorMessage = null,
                    errorOnLoadMore = false
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Logger.e(TAG, e) { "加载历史记录失败" }
            _uiState.update {
                if (it.tab != tab) return@update it
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = e.message ?: "加载历史记录失败",
                    errorOnLoadMore = !reset && !refreshing
                )
            }
        }
    }

    private data class HistoryCache(
        val items: List<HistoryItem>,
        val cursor: HistoryCursor,
        val hasMore: Boolean
    )

    private companion object {
        const val TAG = "HistoryViewModel"
    }
}
