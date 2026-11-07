package com.dhs0319.bills.feature.favorite.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.core.model.FavoriteFolder
import com.dhs0319.bills.feature.favorite.FavoriteScreen
import com.dhs0319.bills.feature.favorite.folderdetail.FavoriteFolderDetailScreen

const val FAVORITE_ROUTE = "favorite"
private const val FAVORITE_FOLDER_FID_ARG = "fid"
internal const val FAVORITE_FOLDER_TITLE_ARG = "title"
internal const val FAVORITE_FOLDER_COUNT_ARG = "mediaCount"
internal const val FAVORITE_FOLDER_CREATED_AT_ARG = "createdAtSec"
private const val FAVORITE_FOLDER_DETAIL_ROUTE =
    "favorite/folder/{$FAVORITE_FOLDER_FID_ARG}" +
        "?$FAVORITE_FOLDER_TITLE_ARG={$FAVORITE_FOLDER_TITLE_ARG}" +
        "&$FAVORITE_FOLDER_COUNT_ARG={$FAVORITE_FOLDER_COUNT_ARG}" +
        "&$FAVORITE_FOLDER_CREATED_AT_ARG={$FAVORITE_FOLDER_CREATED_AT_ARG}"

fun NavController.navigateToFavorite() {
    navigate(FAVORITE_ROUTE)
}

fun NavController.navigateToFavoriteFolder(folder: FavoriteFolder) {
    navigate(
        "favorite/folder/${folder.fid}" +
            "?$FAVORITE_FOLDER_TITLE_ARG=${Uri.encode(folder.title)}" +
            "&$FAVORITE_FOLDER_COUNT_ARG=${folder.mediaCount}" +
            "&$FAVORITE_FOLDER_CREATED_AT_ARG=${folder.createdAtSec}"
    )
}

fun NavGraphBuilder.favoriteScreen(
    onBack: () -> Unit,
    onOpenContent: (FavoriteContentTarget) -> Unit,
    onOpenFolder: (FavoriteFolder) -> Unit,
    onLogin: () -> Unit
) {
    composable(FAVORITE_ROUTE) {
        FavoriteScreen(
            onBack = onBack,
            onOpenContent = onOpenContent,
            onOpenFolder = onOpenFolder,
            onLogin = onLogin
        )
    }
    composable(
        route = FAVORITE_FOLDER_DETAIL_ROUTE,
        arguments = listOf(
            navArgument(FAVORITE_FOLDER_FID_ARG) { type = NavType.LongType },
            navArgument(FAVORITE_FOLDER_TITLE_ARG) {
                type = NavType.StringType
                defaultValue = ""
            },
            navArgument(FAVORITE_FOLDER_COUNT_ARG) {
                type = NavType.IntType
                defaultValue = 0
            },
            navArgument(FAVORITE_FOLDER_CREATED_AT_ARG) {
                type = NavType.LongType
                defaultValue = 0L
            }
        )
    ) { entry ->
        FavoriteFolderDetailScreen(
            onBack = onBack,
            fid = entry.arguments?.getLong(FAVORITE_FOLDER_FID_ARG) ?: 0L,
            onOpenContent = onOpenContent
        )
    }
}
