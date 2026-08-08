package com.dhs0319.bills.feature.home.live

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.LiveRecommendItem
import com.dhs0319.bills.core.model.LiveRecommendUpList

@Immutable
data class HomeLiveUiState(
    val upList: LiveRecommendUpList? = null,
    val items: List<LiveRecommendItem> = emptyList(),
    val isRefreshing: Boolean = true,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null
)
