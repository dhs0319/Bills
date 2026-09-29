package com.dhs0319.bills.feature.home.popular

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.history.WatchLaterRepository
import com.dhs0319.bills.core.model.PopularVideoItem
import com.dhs0319.bills.core.popular.PopularCursor
import com.dhs0319.bills.core.popular.PopularRepository
import com.dhs0319.bills.feature.home.paging.HomePage
import com.dhs0319.bills.feature.home.paging.HomePager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomePopularViewModel @Inject constructor(
    private val repository: PopularRepository,
    private val watchLaterRepository: WatchLaterRepository
) : ViewModel() {
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage = _actionMessage.asStateFlow()
    private val addingToWatchLater = mutableSetOf<Long>()

    private val pager = HomePager<PopularVideoItem, PopularCursor>(
        scope = viewModelScope,
        initialKey = { PopularCursor() },
        itemKey = { it.identityKey },
        loadPage = { cursor ->
            val page = repository.fetchPage(cursor)
            HomePage(
                items = page.items,
                nextKey = page.nextCursor
            )
        },
        onError = { Logger.e("HomePopularViewModel", it) { "加载热门视频失败" } }
    )

    val uiState = pager.state

    fun activate(refreshRequest: Int) = pager.activate(refreshRequest)
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retryLoadMore() = pager.retryLoadMore()

    fun addToWatchLater(aid: Long) {
        if (!addingToWatchLater.add(aid)) return
        viewModelScope.launch {
            try {
                watchLaterRepository.addVideo(aid)
                _actionMessage.value = "已加入稍后再看"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e("HomePopularViewModel", e) { "加入稍后再看失败" }
                _actionMessage.value = e.message?.takeIf(String::isNotBlank) ?: "加入稍后再看失败"
            } finally {
                addingToWatchLater.remove(aid)
            }
        }
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }
}
