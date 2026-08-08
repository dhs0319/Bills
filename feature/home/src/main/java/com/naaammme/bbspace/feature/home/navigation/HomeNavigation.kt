package com.naaammme.bbspace.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.naaammme.bbspace.feature.home.HomeScreen

const val HOME_ROUTE = "home"

fun NavGraphBuilder.homeScreen(
    onNavigateToProfile: () -> Unit
) {
    composable(HOME_ROUTE) {
        HomeScreen(onNavigateToProfile = onNavigateToProfile)
    }
}
