package com.dhs0319.bills.feature.im.feed

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.MsgFeedFilter
import com.dhs0319.bills.core.model.im.MsgFeedCursor
import com.dhs0319.bills.core.model.im.MsgFeedItem

@Immutable
data class MsgFeedUiState(
    val filterType: MsgFeedFilter = MsgFeedFilter.ALL,
    val items: List<MsgFeedItem> = emptyList(),
    val cursor: MsgFeedCursor? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null
)
