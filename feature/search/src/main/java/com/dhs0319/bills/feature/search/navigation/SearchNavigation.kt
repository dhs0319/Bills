package com.dhs0319.bills.feature.search.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dhs0319.bills.core.model.SearchDefaultWord
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.search.SearchScreen

const val SEARCH_ROUTE = "search"
internal const val SEARCH_DEFAULT_SHOW_ARG = "default_show"
internal const val SEARCH_DEFAULT_WORD_ARG = "default_word"

fun NavController.navigateToSearch(defaultWord: SearchDefaultWord? = null) {
    if (defaultWord == null) {
        navigate(SEARCH_ROUTE)
        return
    }
    navigate(
        "$SEARCH_ROUTE?$SEARCH_DEFAULT_SHOW_ARG=${Uri.encode(defaultWord.show)}" +
                "&$SEARCH_DEFAULT_WORD_ARG=${Uri.encode(defaultWord.word)}"
    )
}

fun NavGraphBuilder.searchScreen(
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenVideo: (VideoTarget) -> Unit
) {
    composable(
        route = "$SEARCH_ROUTE?$SEARCH_DEFAULT_SHOW_ARG={$SEARCH_DEFAULT_SHOW_ARG}" +
                "&$SEARCH_DEFAULT_WORD_ARG={$SEARCH_DEFAULT_WORD_ARG}",
        arguments = listOf(
            navArgument(SEARCH_DEFAULT_SHOW_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(SEARCH_DEFAULT_WORD_ARG) {
                type = NavType.StringType
                defaultValue = ""
            }
        )
    ) {
        SearchScreen(
            onBack = onBack,
            onOpenSpace = onOpenSpace,
            onOpenVideo = onOpenVideo
        )
    }
}
