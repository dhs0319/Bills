package com.dhs0319.bills.feature.settings.performance

import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.theme.FrameRateMode
import com.dhs0319.bills.feature.settings.SettingsViewModel
import com.dhs0319.bills.feature.settings.components.SettingDropdown
import com.dhs0319.bills.feature.settings.components.SettingSwitch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val config by viewModel.themeConfig.collectAsStateWithLifecycle()
    val fixBottomBar by viewModel.fixBottomBar.collectAsStateWithLifecycle()
    val playerSettings by viewModel.playerSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val frameRateOptions = remember(context) { context.availableFrameRateModes() }

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("显示设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                FrameRateSelector(
                    selected = config.preferredFrameRate,
                    options = frameRateOptions,
                    onSelect = viewModel::updateFrameRateMode
                )
            }
            item {
                SettingSwitch(
                    title = "固定底栏",
                    subtitle = "主页底栏不再随滚动自动收起",
                    checked = fixBottomBar,
                    onCheckedChange = viewModel::updateFixBottomBar
                )
            }
            item {
                SettingSwitch(
                    title = "状态栏显示时间",
                    subtitle = "在全屏播放器顶部显示当前时间",
                    checked = playerSettings.overlay.showTime,
                    onCheckedChange = viewModel::updatePlayerOverlayShowTime
                )
            }
            item {
                SettingSwitch(
                    title = "状态栏显示网速",
                    subtitle = "在全屏播放器顶部显示视频缓冲速度",
                    checked = playerSettings.overlay.showNetworkSpeed,
                    onCheckedChange = viewModel::updatePlayerOverlayShowNetworkSpeed
                )
            }
            item {
                SettingSwitch(
                    title = "状态栏显示电量",
                    subtitle = "在全屏播放器顶部显示设备电量",
                    checked = playerSettings.overlay.showBattery,
                    onCheckedChange = viewModel::updatePlayerOverlayShowBattery
                )
            }
        }
    }
}

@Composable
private fun FrameRateSelector(
    selected: FrameRateMode,
    options: List<FrameRateMode>,
    onSelect: (FrameRateMode) -> Unit
) {
    val displayedSelection = selected.takeIf { it in options } ?: FrameRateMode.AUTO

    SettingDropdown(
        title = "屏幕刷新率",
        selected = displayedSelection,
        options = options,
        optionLabel = ::frameRateModeLabel,
        onSelect = onSelect
    )
}

private fun Context.availableFrameRateModes(): List<FrameRateMode> {
    val display = getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)
        ?: return listOf(FrameRateMode.AUTO)
    val currentMode = display.mode
    val supportedRefreshRates = display.supportedModes
        .asSequence()
        .filter {
            it.physicalWidth == currentMode.physicalWidth &&
                it.physicalHeight == currentMode.physicalHeight
        }
        .map { it.refreshRate }
        .toList()

    return FrameRateMode.entries.filter { mode ->
        mode == FrameRateMode.AUTO ||
            supportedRefreshRates.any { abs(it - mode.value) < 5f }
    }
}

private fun frameRateModeLabel(mode: FrameRateMode): String = when (mode) {
    FrameRateMode.AUTO -> "自动"
    FrameRateMode.RATE_60 -> "60Hz"
    FrameRateMode.RATE_90 -> "90Hz"
    FrameRateMode.RATE_120 -> "120Hz"
    FrameRateMode.RATE_144 -> "144Hz"
}
