package com.dhs0319.bills.feature.settings.navigation

import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.dhs0319.bills.feature.settings.SettingsScreen
import com.dhs0319.bills.feature.settings.about.AboutScreen
import com.dhs0319.bills.feature.settings.appearance.AppearanceSettingsScreen
import com.dhs0319.bills.feature.settings.audioVideo.AudioVideoSettingsScreen
import com.dhs0319.bills.feature.settings.errorlog.ErrorLogScreen
import com.dhs0319.bills.feature.settings.feed.FeedSettingsScreen
import com.dhs0319.bills.feature.settings.other.OtherSettingsScreen
import com.dhs0319.bills.feature.settings.performance.PerformanceSettingsScreen
import com.dhs0319.bills.feature.settings.privacy.PrivacySettingsScreen

const val SETTINGS_ROUTE = "settings"
const val APPEARANCE_ROUTE = "settings/appearance"
const val PERFORMANCE_ROUTE = "settings/performance"
const val OTHER_SETTINGS_ROUTE = "settings/other"
const val PRIVACY_ROUTE = "settings/privacy"
const val FEED_SETTINGS_ROUTE = "settings/feed"
const val AUDIO_VIDEO_ROUTE = "settings/audio_video"
const val ERROR_LOG_ROUTE = "settings/error_log"
const val ABOUT_ROUTE = "settings/about"
const val HOME_INTEREST_ROUTE = "home/interest"

fun NavGraphBuilder.settingsScreen(
    navController: NavHostController
) {
    composable(SETTINGS_ROUTE) {
        SettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToAppearance = { navController.navigate(APPEARANCE_ROUTE) },
            onNavigateToPerformance = { navController.navigate(PERFORMANCE_ROUTE) },
            onNavigateToOther = { navController.navigate(OTHER_SETTINGS_ROUTE) },
            onNavigateToFeed = { navController.navigate(FEED_SETTINGS_ROUTE) },
            onNavigateToAudioVideo = { navController.navigate(AUDIO_VIDEO_ROUTE) },
            onNavigateToPrivacy = { navController.navigate(PRIVACY_ROUTE) },
            onNavigateToAbout = { navController.navigate(ABOUT_ROUTE) }
        )
    }

    composable(APPEARANCE_ROUTE) {
        AppearanceSettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(PERFORMANCE_ROUTE) {
        PerformanceSettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(OTHER_SETTINGS_ROUTE) {
        OtherSettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(AUDIO_VIDEO_ROUTE) {
        AudioVideoSettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(PRIVACY_ROUTE) {
        PrivacySettingsScreen(onBack = { navController.popBackStack() })
    }

    composable(FEED_SETTINGS_ROUTE) {
        FeedSettingsScreen(
            onBack = { navController.popBackStack() },
            onNavigateToInterest = { navController.navigate(HOME_INTEREST_ROUTE) }
        )
    }

    composable(ERROR_LOG_ROUTE) {
        ErrorLogScreen(onBack = { navController.popBackStack() })
    }

    composable(ABOUT_ROUTE) {
        val context = LocalContext.current
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, 0)
        @Suppress("DEPRECATION")
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P)
            info.longVersionCode else info.versionCode.toLong()
        AboutScreen(
            onBack = { navController.popBackStack() },
            onNavigateToErrorLog = { navController.navigate(ERROR_LOG_ROUTE) },
            versionName = info.versionName ?: "unknown",
            versionCode = versionCode
        )
    }
}
