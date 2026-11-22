package com.dhs0319.bills.feature.search.landing

import com.dhs0319.bills.core.model.SearchSuggestion
import com.dhs0319.bills.feature.search.SearchLoadError

internal data class SearchLandingUiState(
    val trending: List<SearchSuggestion> = emptyList(),
    val discovery: List<SearchSuggestion> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshingDiscovery: Boolean = false,
    val trendingError: SearchLoadError? = null,
    val discoveryError: SearchLoadError? = null
)
