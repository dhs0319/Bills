package com.dhs0319.bills.feature.home.listen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.listen.ListenRepository
import com.dhs0319.bills.core.model.listen.ListenItem
import com.dhs0319.bills.feature.home.paging.HomePage
import com.dhs0319.bills.feature.home.paging.HomePager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ListenHomeViewModel @Inject constructor(
    private val listenRepo: ListenRepository
) : ViewModel() {
    private val pager = HomePager<ListenItem, String>(
        scope = viewModelScope,
        initialKey = { "" },
        itemKey = { it.identityKey },
        loadPage = { token ->
            val result = if (token.isEmpty()) listenRepo.fetchRcmdPlaylist(needTopCards = true)
                else listenRepo.fetchRcmdPlaylistNext(token)
            HomePage(result.items, result.nextPageToken.takeIf { result.hasMore && it.isNotBlank() && it != token })
        },
        onError = { Logger.e("ListenHomeVM", it) { "加载 FM 推荐失败" } }
    )
    val uiState = pager.state

    fun activate(refreshRequest: Int) = pager.activate(refreshRequest)
    fun refresh() = pager.refresh()
    fun loadMore() = pager.loadMore()
    fun retryLoadMore() = pager.retryLoadMore()
}
