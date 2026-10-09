package com.dhs0319.bills.feature.im.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.model.MsgFeedFilter

import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.im.ImRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MsgFeedViewModel @Inject constructor(
    private val imRepo: ImRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MsgFeedUiState())
    val uiState: StateFlow<MsgFeedUiState> = _uiState.asStateFlow()

    private var reqId = 0L

    fun changeFilter(filterType: MsgFeedFilter) {
        if (_uiState.value.filterType == filterType) return
        _uiState.value = MsgFeedUiState(filterType = filterType)
        refresh(forceLoading = true)
    }

    fun initLoad() {
        if (_uiState.value.items.isNotEmpty()) return
        refresh(forceLoading = true)
    }

    fun refresh(forceLoading: Boolean = false) {
        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore) return
        val callId = ++reqId
        _uiState.update {
            it.copy(
                isLoading = forceLoading || it.items.isEmpty(),
                isRefreshing = !forceLoading && it.items.isNotEmpty(),
                isLoadingMore = false,
                errorMessage = null,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(reset = true, callId = callId, state = state)
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.canLoadMore || state.isLoading || state.isRefreshing || state.isLoadingMore) return
        val callId = ++reqId
        _uiState.update {
            it.copy(
                isLoadingMore = true,
                loadMoreError = null
            )
        }
        viewModelScope.launch {
            load(reset = false, callId = callId, state = state)
        }
    }

    private suspend fun load(reset: Boolean, callId: Long, state: MsgFeedUiState) {
        if (callId != reqId) return
        try {
            val page = imRepo.fetchMsgFeedList(
                filterType = state.filterType,
                cursor = if (reset) null else state.cursor
            )
            if (callId != reqId) return
            _uiState.update { it ->
                val newItems = if (reset) {
                    page.msgCards
                } else {
                    it.items + page.msgCards
                }
                val newCursor = page.cursor
                it.copy(
                    items = newItems,
                    cursor = newCursor,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    canLoadMore = newCursor != null && !newCursor.isEnd,
                    errorMessage = null,
                    loadMoreError = null
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (callId != reqId) return
            val msg = e.message.takeIf { !it.isNullOrBlank() } ?: if (reset) "加载失败" else "加载更多失败"
            Logger.e("MsgFeedViewModel", e) { msg }
            _uiState.update {
                if (reset) {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = msg
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        loadMoreError = msg
                    )
                }
            }
        }
    }
}
