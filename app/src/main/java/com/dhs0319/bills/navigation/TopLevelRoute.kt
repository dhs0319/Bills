package com.dhs0319.bills.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Forum as SymbolOutlinedForum
import com.composables.icons.materialsymbols.outlined.Home as SymbolOutlinedHome
import com.composables.icons.materialsymbols.outlined.Motion_photos_on as SymbolOutlinedMotionPhotosOn
import com.composables.icons.materialsymbols.outlined.Person as SymbolOutlinedPerson
import com.composables.icons.materialsymbols.outlinedfilled.Forum as SymbolOutlinedFilledForum
import com.composables.icons.materialsymbols.outlinedfilled.Home as SymbolOutlinedFilledHome
import com.composables.icons.materialsymbols.outlinedfilled.Motion_photos_on as SymbolOutlinedFilledMotionPhotosOn
import com.composables.icons.materialsymbols.outlinedfilled.Person as SymbolOutlinedFilledPerson

enum class TopLevelRoute(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val route: String
) {
    HOME("首页", MaterialSymbols.Outlined.SymbolOutlinedHome, MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledHome, "home"),
    DYNAMIC("动态", MaterialSymbols.Outlined.SymbolOutlinedMotionPhotosOn, MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledMotionPhotosOn, "dynamic"),
    MESSAGE("消息", MaterialSymbols.Outlined.SymbolOutlinedForum, MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledForum, "message"),
    PROFILE("我的", MaterialSymbols.Outlined.SymbolOutlinedPerson, MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledPerson, "profile")
}
