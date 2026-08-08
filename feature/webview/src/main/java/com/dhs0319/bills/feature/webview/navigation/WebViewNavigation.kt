package com.dhs0319.bills.feature.webview.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.WebLinkTarget
import com.dhs0319.bills.feature.webview.WebViewRoute
import com.dhs0319.bills.feature.webview.WebViewScreen

const val WEBVIEW_ROUTE = "webview"
private const val URL_ARG = "url"

fun NavController.navigateToWebView(url: String) {
    val encoded = Uri.encode(url)
    navigate("$WEBVIEW_ROUTE?$URL_ARG=$encoded")
}

fun NavGraphBuilder.webViewScreen(
    onBack: () -> Unit,
    onOpenVideo: (WebLinkTarget.ToVideo) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenLive: (LiveRoute) -> Unit,
    onOpenExternal: (String) -> Unit
) {
    composable(
        route = "$WEBVIEW_ROUTE?$URL_ARG={$URL_ARG}",
        arguments = listOf(
            navArgument(URL_ARG) { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val url = backStackEntry.arguments?.getString(URL_ARG).orEmpty()
        WebViewScreen(
            url = url,
            onBack = onBack,
            onOpenVideo = onOpenVideo,
            onOpenSpace = onOpenSpace,
            onOpenLive = onOpenLive,
            onOpenExternal = onOpenExternal
        )
    }
}
