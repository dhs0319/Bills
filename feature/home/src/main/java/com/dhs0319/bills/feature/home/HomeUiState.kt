package com.dhs0319.bills.feature.home

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.FeedItem
import com.dhs0319.bills.core.model.InterestChoose
import com.dhs0319.bills.feature.home.paging.HomePagingState

@Immutable
data class HomeUiState(
    val paging: HomePagingState<FeedItem> = HomePagingState(),
    val interestChoose: InterestChoose? = null,
    val toastMessage: String = "",
    val dislikedReasons: Map<String, String> = emptyMap()
)
