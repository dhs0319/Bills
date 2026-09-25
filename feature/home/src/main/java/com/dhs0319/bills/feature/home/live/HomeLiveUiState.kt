package com.dhs0319.bills.feature.home.live

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.LiveRecommendItem
import com.dhs0319.bills.core.model.LiveRecommendUpList
import com.dhs0319.bills.feature.home.paging.HomePagingState

@Immutable
data class HomeLiveUiState(
    val upList: LiveRecommendUpList? = null,
    val paging: HomePagingState<LiveRecommendItem> = HomePagingState()
)
