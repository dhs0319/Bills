package com.dhs0319.bills.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Contrast
import com.composables.icons.materialsymbols.outlinedfilled.Manage_accounts
import com.composables.icons.materialsymbols.outlinedfilled.Moon_stars
import com.composables.icons.materialsymbols.outlinedfilled.Settings
import com.composables.icons.materialsymbols.outlinedfilled.Wb_sunny
import com.dhs0319.bills.core.designsystem.theme.ThemeMode
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.feature.user.collage.UserCollageSection
import com.dhs0319.bills.feature.user.collage.rememberUserCollageBackgroundBrush
import com.dhs0319.bills.feature.user.collage.rememberUserCollagePalette
import com.dhs0319.bills.feature.user.component.AccountExpiredDialog

enum class UserDest {
    History,
    Favorite,
    WatchLater
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    onNavigateToAccount: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigate: (UserDest) -> Unit,
    onNavigateToDownload: () -> Unit,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit = {},
    vm: UserViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val palette = rememberUserCollagePalette()
    val backgroundBrush = rememberUserCollageBackgroundBrush(palette)

    Scaffold(
        modifier = Modifier.background(brush = backgroundBrush),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("我的") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                actions = {
                    ThemeModeSelector(
                        selected = themeMode,
                        onSelect = onThemeModeChange
                    )
                    IconButton(onClick = onNavigateToAccount) {
                        Icon(MaterialSymbols.OutlinedFilled.Manage_accounts, contentDescription = "账号管理")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(MaterialSymbols.OutlinedFilled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        UserCollageSection(
            user = state.user,
            onNavigateToAccount = onNavigateToAccount,
            onOpenSpace = onOpenSpace,
            onNavigate = onNavigate,
            onNavigateToDownload = onNavigateToDownload,
            palette = palette,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
    }

    if (state.showAccountExpiredDialog) {
        AccountExpiredDialog(onDismiss = vm::dismissAccountExpiredDialog)
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val (icon, label) = when (selected) {
        ThemeMode.LIGHT -> MaterialSymbols.OutlinedFilled.Wb_sunny to "浅色主题"
        ThemeMode.DARK -> MaterialSymbols.OutlinedFilled.Moon_stars to "深色主题"
        ThemeMode.SYSTEM -> MaterialSymbols.OutlinedFilled.Contrast to "跟随系统主题"
    }

    IconButton(
        onClick = {
            onSelect(
                when (selected) {
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.DARK -> ThemeMode.SYSTEM
                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                }
            )
        },
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "$label，点击切换"
        )
    }
}
