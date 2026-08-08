package com.naaammme.bbspace.feature.dynamic

import androidx.compose.runtime.Immutable
import com.naaammme.bbspace.core.model.DynamicCursor
import com.naaammme.bbspace.core.model.DynamicItem
import com.naaammme.bbspace.core.model.DynamicUpList

@Immutable
data class DynamicUiState(
    val upList: DynamicUpList? = null,
    val items: List<DynamicItem> = emptyList(),
    val cursor: DynamicCursor = DynamicCursor(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
    val loadMoreError: String? = null,
    val isLoggedIn: Boolean = true
) {
    val canLoadMore: Boolean
        get() = hasMore && items.isNotEmpty() && !isLoading && !isRefreshing && !isLoadingMore
}
