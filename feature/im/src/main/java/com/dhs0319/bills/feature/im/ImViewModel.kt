package com.dhs0319.bills.feature.im

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.im.ImRepository
import com.dhs0319.bills.core.model.ImPaginationParams
import com.dhs0319.bills.core.model.ImSessionItem
import com.dhs0319.bills.core.model.ImSessionTab
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ImViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val imRepo: ImRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImUiState())
    val uiState: StateFlow<ImUiState> = _uiState.asStateFlow()
    private val cache = mutableMapOf<ImSessionTab, ImTabCache>()
    private var reqId = 0L
    private var handledSessionListVersion = 0L

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow.collect { mid ->
                reqId += 1L
                if (mid > 0) {
                    _uiState.update { state ->
                        state.copy(
                            isLoggedIn = true,
                            errorMessage = null,
                            loadMoreError = null
                        )
                    }
                    val state = _uiState.value
                    if (state.sessions.isNotEmpty() || cache.isNotEmpty()) return@collect
                    refresh(forceLoading = true)
                } else {
                    cache.clear()
                    _uiState.value = ImUiState(isLoggedIn = false)
                }
            }
        }
        viewModelScope.launch {
            imRepo.sessionListVersion.collect { refreshIfSessionListChanged() }
        }
    }

    fun selectTab(tab: ImSessionTab) {
        val state = _uiState.value
        if (state.currentTab == tab || state.isLoading || state.isRefreshing || state.isLoadingMore) return
        cache[tab]?.let { cached ->
            _uiState.value = state.copy(
                tabs = ImSessionTab.entries,
                currentTab = tab,
                sessions = cached.sessions,
                paginationParams = cached.paginationParams,
                isLoading = false,
                isRefreshing = false,
                isLoadingMore = false,
                errorMessage = null,
                loadMoreError = null
            )
            return
        }
        reqId += 1L
        _uiState.update {
            it.copy(
                currentTab = tab,
                sessions = emptyList(),
                isLoading = true,
                isRefreshing = false,
                isLoadingMore = false,
                paginationParams = null,
                errorMessage = null,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(tab, null, true)
        }
    }

    fun refresh(forceLoading: Boolean = false) {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isLoading || state.isRefreshing || state.isLoadingMore) return
        reqId += 1L
        _uiState.update {
            it.copy(
                isLoading = forceLoading || it.sessions.isEmpty(),
                isRefreshing = !forceLoading && it.sessions.isNotEmpty(),
                isLoadingMore = false,
                errorMessage = null,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(state.currentTab, null, true)
        }
    }

    fun refreshIfSessionListChanged() {
        val version = imRepo.sessionListVersion.value
        if (version == handledSessionListVersion) return
        handledSessionListVersion = version
        refreshSilently()
    }

    private fun refreshSilently() {
        val state = _uiState.value
        if (!state.isLoggedIn || state.isLoading || state.isRefreshing || state.isLoadingMore) return
        reqId += 1L
        viewModelScope.launch {
            load(state.currentTab, null, true, silent = true)
        }
    }

    fun togglePin(item: ImSessionItem) {
        val sessionId = item.sessionId ?: return
        val target = !item.isPinned
        if (target && !item.canPin) return
        if (!target && !item.canUnpin) return
        val tab = _uiState.value.currentTab
        _uiState.update { it.copy(actionError = null) }
        mutateSessions(tab) { list -> list.withPinned(item.key, target) }
        viewModelScope.launch {
            try {
                imRepo.setSessionPinned(sessionId, target)
            } catch (e: Exception) {
                mutateSessions(tab) { list -> list.withPinned(item.key, item.isPinned) }
                _uiState.update { it.copy(actionError = e.userMessage(PIN_ERR)) }
            }
        }
    }

    fun deleteSession(item: ImSessionItem) {
        val sessionId = item.sessionId ?: return
        if (!item.canDelete) return
        val tab = _uiState.value.currentTab
        val snapshot = _uiState.value.sessions
        _uiState.update { it.copy(actionError = null) }
        mutateSessions(tab) { list -> list.filterNot { it.key == item.key } }
        viewModelScope.launch {
            try {
                imRepo.deleteSession(sessionId)
            } catch (e: Exception) {
                cache[tab] = ImTabCache(snapshot, cache[tab]?.paginationParams)
                if (_uiState.value.currentTab == tab) {
                    _uiState.update { it.copy(sessions = snapshot) }
                }
                _uiState.update { it.copy(actionError = e.userMessage(DELETE_ERR)) }
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    private fun mutateSessions(
        tab: ImSessionTab,
        transform: (List<ImSessionItem>) -> List<ImSessionItem>
    ) {
        val cached = cache[tab]
        val base = if (_uiState.value.currentTab == tab) _uiState.value.sessions else cached?.sessions
        if (base == null) return
        val updated = transform(base)
        cache[tab] = ImTabCache(
            sessions = updated,
            paginationParams = cached?.paginationParams ?: _uiState.value.paginationParams
        )
        if (_uiState.value.currentTab == tab) {
            _uiState.update { it.copy(sessions = updated) }
        }
    }

    private fun List<ImSessionItem>.withPinned(key: String, pinned: Boolean): List<ImSessionItem> {
        val target = firstOrNull { it.key == key } ?: return this
        val others = filterNot { it.key == key }
        val pinnedItems = others.filter { it.isPinned }
        val unpinnedItems = others.filterNot { it.isPinned }
        return if (pinned) {
            listOf(target.copy(isPinned = true)) + pinnedItems + unpinnedItems
        } else {
            val moved = target.copy(isPinned = false)
            val insertAt = unpinnedItems
                .indexOfFirst { it.timeMicros < moved.timeMicros }
                .let { if (it < 0) unpinnedItems.size else it }
            pinnedItems + unpinnedItems.toMutableList().apply { add(insertAt, moved) }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        val nextParams = state.paginationParams ?: return
        if (!state.canLoadMore) return
        reqId += 1L
        _uiState.update {
            it.copy(
                isLoadingMore = true,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(state.currentTab, nextParams, false)
        }
    }

    private suspend fun load(
        tab: ImSessionTab,
        paginationParams: ImPaginationParams?,
        reset: Boolean,
        silent: Boolean = false
    ) {
        val callId = reqId
        try {
            val page = imRepo.fetchSessions(
                tab = tab,
                paginationParams = paginationParams
            )
            if (callId != reqId) return
            _uiState.update { state ->
                if (state.currentTab != tab) return@update state
                val sessions = mergeSessions(state.sessions, page.sessions, reset)
                cache[tab] = ImTabCache(
                    sessions = sessions,
                    paginationParams = page.paginationParams
                )
                state.copy(
                    tabs = page.tabs,
                    currentTab = page.currentTab,
                    sessions = sessions,
                    paginationParams = page.paginationParams,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = null,
                    loadMoreError = null,
                    isLoggedIn = true
                )
            }
        } catch (e: Exception) {
            if (callId != reqId) return
            val msg = e.userMessage(
                default = if (reset) LOAD_ERR else LOAD_MORE_ERR
            )
            Logger.e(TAG, e) { msg }
            if (silent) return
            _uiState.update { state ->
                if (state.currentTab != tab) return@update state
                if (reset) {
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg
                    )
                } else {
                    state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        loadMoreError = msg
                    )
                }
            }
        }
    }

    private fun Throwable.userMessage(
        default: String
    ): String {
        return message?.takeIf(String::isNotBlank) ?: default
    }

    private fun mergeSessions(
        current: List<ImSessionItem>,
        incoming: List<ImSessionItem>,
        reset: Boolean
    ): List<ImSessionItem> {
        if (reset) return incoming
        if (current.isEmpty()) return incoming
        if (incoming.isEmpty()) return current
        val merged = LinkedHashMap<String, ImSessionItem>(current.size + incoming.size)
        current.forEach { merged[it.key] = it }
        incoming.forEach { merged[it.key] = it }
        return merged.values.toList()
    }

    private data class ImTabCache(
        val sessions: List<ImSessionItem>,
        val paginationParams: ImPaginationParams?
    )

    private companion object {
        const val TAG = "ImViewModel"
        const val PIN_ERR = "置顶操作失败"
        const val DELETE_ERR = "删除会话失败"
        const val LOAD_ERR = "加载消息列表失败"
        const val LOAD_MORE_ERR = "加载更多消息失败"
    }
}
