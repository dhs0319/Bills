package com.dhs0319.bills.feature.bbspace.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.bbspace.BbSpaceScreen

const val BBSPACE_ROUTE = "bbspace"

fun NavController.navigateToBbSpace() {
    navigate(BBSPACE_ROUTE)
}

fun NavGraphBuilder.bbSpaceScreen(
    navController: NavHostController,
    onOpenSpace: (SpaceRoute) -> Unit = {},
    onOpenVideoDetail: (VideoTarget) -> Unit = {},
    onOpenDynamicDetail: (String) -> Unit = {},
    onOpenLiveDetail: (LiveRoute) -> Unit = {}
) {
    composable(BBSPACE_ROUTE) {
        BbSpaceScreen(
            onBack = { navController.popBackStack() },
            onOpenSpace = onOpenSpace,
            onOpenVideoDetail = onOpenVideoDetail,
            onOpenDynamicDetail = onOpenDynamicDetail,
            onOpenLiveDetail = onOpenLiveDetail
        )
    }
}
