package com.dhs0319.bills.feature.im.likes

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.im.MsgFeedCursor
import com.dhs0319.bills.core.model.im.ReceivedLikeItem
import com.dhs0319.bills.core.model.im.ReceivedLikesPage

@Immutable
data class ReceivedLikesUiState(
    val isLoggedIn: Boolean = false,
    val items: List<ReceivedLikeItem> = emptyList(),
    val cursor: MsgFeedCursor? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val notice: String? = null
) {
    val canLoadMore: Boolean get() = cursor?.isEnd == false
    val isBusy: Boolean get() = isLoading || isRefreshing || isLoadingMore

    internal fun withPage(page: ReceivedLikesPage, reset: Boolean): ReceivedLikesUiState = copy(
        items = (if (reset) page.items else items + page.items).distinctBy { it.key },
        // Stop if the server repeats a cursor, even when it claims there are more items.
        cursor = page.cursor.takeUnless { !reset && it == cursor },
        isLoading = false,
        isRefreshing = false,
        isLoadingMore = false,
        errorMessage = null,
        loadMoreError = null,
        notice = page.notice
    )
}
