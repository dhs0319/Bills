package com.naaammme.bbspace.feature.home

import androidx.compose.runtime.Immutable
import com.naaammme.bbspace.core.model.FeedItem
import com.naaammme.bbspace.core.model.InterestChoose

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
