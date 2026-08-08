package com.dhs0319.bills.feature.favorite.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.feature.favorite.FavoriteScreen
import com.dhs0319.bills.feature.favorite.folderdetail.FavoriteFolderDetailScreen

const val FAVORITE_ROUTE = "favorite"
private const val FAVORITE_FOLDER_FID_ARG = "fid"
private const val FAVORITE_FOLDER_DETAIL_ROUTE = "favorite/folder/{$FAVORITE_FOLDER_FID_ARG}"

fun NavController.navigateToFavorite() {
    navigate(FAVORITE_ROUTE)
}

fun NavController.navigateToFavoriteFolder(fid: Long) {
    navigate("favorite/folder/$fid")
}

fun NavGraphBuilder.favoriteScreen(
    onBack: () -> Unit,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onOpenFolder: (Long) -> Unit
) {
    composable(FAVORITE_ROUTE) {
        FavoriteScreen(
            onBack = onBack,
            onOpenContent = onOpenContent,
            onOpenFolder = onOpenFolder
        )
    }
    composable(
        route = FAVORITE_FOLDER_DETAIL_ROUTE,
        arguments = listOf(
            navArgument(FAVORITE_FOLDER_FID_ARG) { type = androidx.navigation.NavType.LongType }
        )
    ) { entry ->
        FavoriteFolderDetailScreen(
            onBack = onBack,
            fid = entry.arguments?.getLong(FAVORITE_FOLDER_FID_ARG) ?: 0L,
            onOpenContent = onOpenContent
        )
    }
}
