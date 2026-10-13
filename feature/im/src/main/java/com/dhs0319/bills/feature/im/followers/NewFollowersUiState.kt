package com.dhs0319.bills.feature.im.followers

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.im.NewFollowerItem
import com.dhs0319.bills.core.model.im.NewFollowersPage

@Immutable
data class NewFollowersUiState(
    val isLoggedIn: Boolean = false,
    val items: List<NewFollowerItem> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val followingMids: Set<Long> = emptySet(),
    val pendingUnfollowMid: Long? = null,
    val followError: String? = null
) {
    val isBusy: Boolean get() = isLoading || isRefreshing || isLoadingMore

    internal fun withPage(result: NewFollowersPage, requestedPage: Int): NewFollowersUiState {
        val reset = requestedPage == 1
        val merged = (if (reset) result.items else items + result.items).distinctBy { it.mid }
        return copy(
            items = merged,
            page = requestedPage,
            // Avoid repeatedly requesting pages if the server repeats the same followers.
            hasMore = result.hasMore && result.items.isNotEmpty() && (reset || merged.size > items.size),
            isLoading = false,
            isRefreshing = false,
            isLoadingMore = false,
            errorMessage = null,
            loadMoreError = null
        )
    }
}
