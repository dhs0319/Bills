package com.dhs0319.bills.feature.home

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.FeedItem
import com.dhs0319.bills.core.model.InterestChoose

@Immutable
data class HomeUiState(
    val items: List<FeedItem> = emptyList(),
    val interestChoose: InterestChoose? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val errorMessage: String? = null,
    val toastMessage: String = "",
    val dislikedReasons: Map<String, String> = emptyMap()
)
