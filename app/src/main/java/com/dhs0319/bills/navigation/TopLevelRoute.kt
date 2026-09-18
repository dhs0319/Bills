package com.dhs0319.bills.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MotionPhotosOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopLevelRoute(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String
) {
    HOME("首页", Icons.Outlined.Home, Icons.Default.Home, "home"),
    DYNAMIC("动态", Icons.Outlined.MotionPhotosOn, Icons.Default.MotionPhotosOn, "dynamic"),
    MESSAGE("消息", Icons.Outlined.Email, Icons.Default.Email, "message"),
    PROFILE("我的", Icons.Outlined.Person, Icons.Default.Person, "profile")
}
