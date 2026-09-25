package com.dhs0319.bills.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.feed.FeedDislikeRepository
import com.dhs0319.bills.core.feed.PageActionTracker
import com.dhs0319.bills.core.feed.FeedRepository
import com.dhs0319.bills.core.settings.AppSettings
import com.dhs0319.bills.core.model.FeedItem
import com.dhs0319.bills.core.model.ThreePointReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.dhs0319.bills.feature.home.paging.HomePage
import com.dhs0319.bills.feature.home.paging.HomePager
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val feedRepo: FeedRepository,
    private val feedDislikeRepo: FeedDislikeRepository,
    private val appSettings: AppSettings,
    private val pageActionTracker: PageActionTracker
) : ViewModel() {

    companion object {
        private const val TAG = "HomeViewModel"
    }

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private data class Interest(val id: Int, val result: String, val posIds: String)
    private data class FeedKey(val idx: Long, val flush: Int = 0, val pull: Boolean = true, val interest: Interest? = null)
    private var pendingInterest: Interest? = null

    private val pager = HomePager<FeedItem, FeedKey>(
        scope = viewModelScope,
        initialKey = {
            FeedKey(if (pendingInterest != null) 0L else _uiState.value.paging.items.firstOrNull()?.idx ?: 0L,
                interest = pendingInterest)
        },
        itemKey = { "${it.goto}|${it.param}" },
        loadPage = { key ->
            val feed = key.interest?.let { interest ->
                feedRepo.fetchFeedWithInterest(key.idx, key.pull, key.flush,
                    interest.id, interest.result, interestPosIds = interest.posIds)
            } ?: feedRepo.fetchFeed(key.idx, key.pull, key.flush)
            val interestChoose = feed.interestChoose?.takeUnless { appSettings.interestDone.first() }
            HomePage(feed.items, feed.nextIdx?.takeIf { key.pull || it != key.idx }
                ?.let { FeedKey(it, key.flush + 1, pull = false) }) {
                if (key.interest != null) {
                    pendingInterest = null
                    viewModelScope.launch { appSettings.markInterestDone() }
                }
                _uiState.update {
                    it.copy(
                        interestChoose = if (key.interest != null) null else interestChoose ?: it.interestChoose,
                        dislikedReasons = if (key.pull) emptyMap() else it.dislikedReasons
                    )
                }
            }
        },
        onStateChanged = { paging -> _uiState.update { it.copy(paging = paging) } },
        onError = { Logger.e(TAG, it) { "加载推荐失败" } }
    )

    private fun FeedItem.actionKey(): String {
        return "$goto|$param|$idx"
    }

    fun refreshPageAction() {
        pageActionTracker.refresh()
    }

    init {
        viewModelScope.launch {
            feedRepo.toastFlow.collect { toast ->
                if (toast.hasToast) {
                    _uiState.update { it.copy(toastMessage = toast.message) }
                }
            }
        }
    }

    fun activate(refreshRequest: Int) = pager.activate(refreshRequest)
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retryLoadMore() = pager.retryLoadMore()

    fun dismissInterest() {
        _uiState.update { it.copy(interestChoose = null) }
    }

    fun submitInterest(interestId: Int, interestResult: String, interestPosIds: String) {
        pendingInterest = Interest(interestId, interestResult, interestPosIds)
        _uiState.update { it.copy(interestChoose = null) }
        pager.refresh(restart = true)
    }

    fun submitDislike(item: FeedItem, reason: ThreePointReason) {
        val context = item.dislikeContext ?: run {
            _uiState.update { it.copy(toastMessage = "当前卡片暂不支持此操作") }
            return
        }
        val itemKey = item.actionKey()
        viewModelScope.launch {
            runCatching { feedDislikeRepo.dislike(context, reason) }
                .onSuccess { result ->
                    _uiState.update { state ->
                        state.copy(
                            dislikedReasons = state.dislikedReasons + (itemKey to reason.name),
                            toastMessage = result.toast
                        )
                    }
                }
                .onFailure { e ->
                    Logger.e(TAG, e as? Exception) { "提交不感兴趣失败" }
                    _uiState.update { state ->
                        state.copy(
                            toastMessage = e.message ?: "提交失败"
                        )
                    }
                }
        }
    }

    fun cancelDislike(item: FeedItem) {
        val context = item.dislikeContext ?: run {
            _uiState.update { it.copy(toastMessage = "当前卡片暂不支持此操作") }
            return
        }
        val itemKey = item.actionKey()
        viewModelScope.launch {
            runCatching { feedDislikeRepo.cancelDislike(context) }
                .onSuccess { result ->
                    _uiState.update { state ->
                        state.copy(
                            dislikedReasons = state.dislikedReasons - itemKey,
                            toastMessage = result.toast
                        )
                    }
                }
                .onFailure { e ->
                    Logger.e(TAG, e as? Exception) { "撤回不感兴趣失败" }
                    _uiState.update { state ->
                        state.copy(
                            toastMessage = e.message ?: "撤回失败"
                        )
                    }
                }
        }
    }

    fun consumeToast() {
        if (_uiState.value.toastMessage.isEmpty()) return
        _uiState.update { it.copy(toastMessage = "") }
    }
}
