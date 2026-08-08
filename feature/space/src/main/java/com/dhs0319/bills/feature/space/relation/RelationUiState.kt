package com.dhs0319.bills.feature.space.relation

import com.dhs0319.bills.core.model.RelationUser

data class RelationTabState(
    val users: List<RelationUser> = emptyList(),
    val page: Int = 1,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasMore: Boolean = true,
    val error: String? = null
)

data class RelationUiState(
    val selectedTab: Int = 0, // 0 for followings, 1 for fans
    val followingsState: RelationTabState = RelationTabState(),
    val fansState: RelationTabState = RelationTabState()
) {
    val currentTabState: RelationTabState
        get() = if (selectedTab == 1) fansState else followingsState
}
