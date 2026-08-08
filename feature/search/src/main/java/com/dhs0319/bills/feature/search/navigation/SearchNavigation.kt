package com.dhs0319.bills.feature.search.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.search.SearchScreen

const val SEARCH_ROUTE = "search"

fun NavController.navigateToSearch() {
    navigate(SEARCH_ROUTE)
}

fun NavGraphBuilder.searchScreen(
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenVideo: (VideoTarget) -> Unit
) {
    composable(SEARCH_ROUTE) {
        SearchScreen(
            onBack = onBack,
            onOpenSpace = onOpenSpace,
            onOpenVideo = onOpenVideo
        )
    }
}
