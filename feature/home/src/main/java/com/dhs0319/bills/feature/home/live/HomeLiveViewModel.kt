package com.dhs0319.bills.feature.home.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.live.LiveRecommendRepository
import com.dhs0319.bills.core.model.LiveRecommendItem
import com.dhs0319.bills.core.model.LiveRecommendUpList
import com.dhs0319.bills.feature.home.paging.HomePage
import com.dhs0319.bills.feature.home.paging.HomePager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class HomeLiveViewModel @Inject constructor(
    private val repository: LiveRecommendRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeLiveUiState())
    val uiState = _uiState.asStateFlow()
    private var hasLoaded = false
    private data class PendingUpList(val value: LiveRecommendUpList?)
    private var pendingUpList: PendingUpList? = null

    private val pager = HomePager<LiveRecommendItem, Int>(
        scope = viewModelScope,
        initialKey = { 1 },
        itemKey = { it.roomId },
        loadPage = { number ->
            val page = repository.fetchRecommendPage(
                page = number,
                relationPage = 1,
                isRefresh = number == 1 && hasLoaded,
                loginEvent = if (hasLoaded) 0 else 1
            )
            HomePage(page.items, (number + 1).takeIf { page.hasMore }) {
                hasLoaded = true
                if (number == 1 || _uiState.value.upList == null) {
                    pendingUpList = PendingUpList(page.upList)
                }
            }
        },
        onStateChanged = { paging ->
            val upListUpdate = pendingUpList
            pendingUpList = null
            _uiState.update {
                it.copy(
                    paging = paging,
                    upList = if (upListUpdate != null) upListUpdate.value else it.upList
                )
            }
        },
        onError = { Logger.e("HomeLiveViewModel", it) { "加载直播推荐失败" } }
    )

    fun activate(refreshRequest: Int) = pager.activate(refreshRequest)
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retryLoadMore() = pager.retryLoadMore()
}
