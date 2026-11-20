package com.dhs0319.bills.feature.search.landing

import com.dhs0319.bills.core.model.SearchSuggestion

data class SearchLandingUiState(
    val trending: List<SearchSuggestion> = emptyList(),
    val discovery: List<SearchSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshingDiscovery: Boolean = false,
    val trendingError: String? = null,
    val discoveryError: String? = null
)
