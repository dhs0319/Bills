package com.naaammme.bbspace.feature.im

import androidx.compose.runtime.Immutable
import com.naaammme.bbspace.core.model.ImPaginationParams
import com.naaammme.bbspace.core.model.ImSessionItem
import com.naaammme.bbspace.core.model.ImSessionTab

@Immutable
data class ImUiState(
    val tabs: List<ImSessionTab> = ImSessionTab.entries,
    val currentTab: ImSessionTab = ImSessionTab.DEFAULT,
    val sessions: List<ImSessionItem> = emptyList(),
    val paginationParams: ImPaginationParams? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val isLoggedIn: Boolean = true
) {
    val canLoadMore: Boolean
        get() = paginationParams?.hasMore == true &&
            sessions.isNotEmpty() &&
            !isLoading &&
            !isRefreshing &&
            !isLoadingMore
}
