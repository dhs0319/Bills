package com.dhs0319.bills.feature.settings.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dhs0319.bills.feature.settings.SettingsViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.theme.AnimationSpeed
import com.dhs0319.bills.core.designsystem.theme.CornerStyle
import com.dhs0319.bills.core.designsystem.theme.DEFAULT_PULL_REFRESH_DISTANCE_DP
import com.dhs0319.bills.core.designsystem.theme.PresetColors
import com.dhs0319.bills.core.designsystem.theme.ThemeMode
import com.dhs0319.bills.core.designsystem.theme.TransitionStyle
import com.dhs0319.bills.core.designsystem.theme.previewThemePrimaryColor
import com.dhs0319.bills.feature.settings.components.SettingDropdown
import com.dhs0319.bills.feature.settings.components.SettingSwitch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val config by viewModel.themeConfig.collectAsStateWithLifecycle()
    val systemIsDark = isSystemInDarkTheme()
    var colorPickerVisible by rememberSaveable { mutableStateOf(false) }
    val previewIsDark = when (config.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> systemIsDark
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("外观设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                }
            )
        },
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
                ThemeModeSelector(
                    selected = config.themeMode,
                    onSelect = viewModel::updateThemeMode
                )
            }

            item {
                SettingSwitch(
                    title = "使用系统动态取色",
                    subtitle = "开启后由壁纸决定应用配色",
                    checked = config.useDynamicColor,
                    onCheckedChange = viewModel::updateUseDynamicColor
                )
            }

            if (!config.useDynamicColor) {
                item {
                    ThemeColorSetting(
                        selected = config.seedColor,
                        isDark = previewIsDark,
                        onClick = { colorPickerVisible = true }
                    )
                }
            }

            item {
                FontScaleSelector(
                    scale = config.fontScale,
                    onScaleChange = viewModel::updateFontScale
                )
            }

            item {
                UiScaleSelector(
                    scale = config.uiScale,
                    onScaleChange = viewModel::updateUiScale
                )
            }

            item {
                RoundScreenSafePaddingSelector(
                    scale = config.roundScreenSafePaddingScale,
                    onScaleChange = viewModel::updateRoundScreenSafePaddingScale
                )
            }

            item {
                PullRefreshDistanceSelector(
                    distanceDp = config.pullRefreshDistanceDp,
                    onDistanceChange = viewModel::updatePullRefreshDistance
                )
            }

            item {
                AnimationSpeedSelector(
                    speed = config.animationSpeed,
                    onSelect = viewModel::updateAnimationSpeed
                )
            }

            item {
                CornerStyleSelector(
                    selected = config.cornerStyle,
                    onSelect = viewModel::updateCornerStyle
                )
            }

            item {
                TransitionStyleSelector(
                    selected = config.transitionStyle,
                    onSelect = viewModel::updateTransitionStyle
                )
            }
        }
    }

    if (colorPickerVisible && !config.useDynamicColor) {
        AlertDialog(
            onDismissRequest = { colorPickerVisible = false },
            title = { Text("主题色") },
            text = {
                ColorPaletteGrid(
                    selected = config.seedColor,
                    isDark = previewIsDark,
                    onSelect = { color ->
                        viewModel.updateSeedColor(color)
                        colorPickerVisible = false
                    }
                )
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ThemeModeSelector(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    SettingDropdown(
        title = "主题模式",
        selected = selected,
        options = ThemeMode.entries,
        optionLabel = ::themeModeLabel,
        onSelect = onSelect
    )
}

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.LIGHT -> "浅色"
    ThemeMode.DARK -> "深色"
    ThemeMode.SYSTEM -> "跟随系统"
}

@Composable
private fun ThemeColorSetting(
    selected: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val previewColor = previewThemePrimaryColor(selected, isDark)

    Card {
        ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = { Text("主题色", style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text("点击切换") },
            trailingContent = {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(previewColor)
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun ColorPaletteGrid(
    selected: Color,
    isDark: Boolean,
    onSelect: (Color) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PresetColors.forEach { (color, _) ->
            ColorItem(
                color = previewThemePrimaryColor(color, isDark),
                selected = color == selected,
                onClick = { onSelect(color) }
            )
        }
    }
}

@Composable
private fun ColorItem(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private val FONT_SCALES = listOf(0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f)
private val UI_SCALES = listOf(0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f, 1.1f)
private val ROUND_SCREEN_SAFE_PADDING_SCALES = listOf(
    0f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f,
    0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f
)
private val PULL_REFRESH_DISTANCES = listOf(
    16f, 24f, 32f, 40f, 48f, 56f, 64f, 72f, 80f,
    88f, 96f, 104f, 112f, 120f, 128f, 136f, 144f, 152f, 160f
)

private fun sliderIndex(value: Float, lastIndex: Int): Int {
    return value.roundToInt().coerceIn(0, lastIndex)
}

@Composable
private fun FontScaleSelector(
    scale: Float,
    onScaleChange: (Float) -> Unit
) {
    val idx = remember(scale) {
        FONT_SCALES.indexOfFirst { kotlin.math.abs(it - scale) < 0.01f }
            .coerceAtLeast(0)
    }
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("字体大小", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${(FONT_SCALES[idx] * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = idx.toFloat(),
                onValueChange = { onScaleChange(FONT_SCALES[sliderIndex(it, FONT_SCALES.lastIndex)]) },
                valueRange = 0f..(FONT_SCALES.size - 1).toFloat(),
                steps = FONT_SCALES.size - 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("小", style = MaterialTheme.typography.bodySmall)
                Text("标准", style = MaterialTheme.typography.bodySmall)
                Text("大", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun UiScaleSelector(
    scale: Float,
    onScaleChange: (Float) -> Unit
) {
    val idx = remember(scale) {
        UI_SCALES.indexOfFirst { kotlin.math.abs(it - scale) < 0.01f }
            .takeIf { it >= 0 }
            ?: UI_SCALES.indexOf(1.0f).coerceAtLeast(0)
    }
    var sliderIdx by remember(scale) { mutableIntStateOf(idx) }
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("界面缩放", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "调整界面元素的整体大小",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(UI_SCALES[sliderIdx] * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderIdx.toFloat(),
                onValueChange = { sliderIdx = sliderIndex(it, UI_SCALES.lastIndex) },
                onValueChangeFinished = {
                    val newScale = UI_SCALES[sliderIdx]
                    if (kotlin.math.abs(newScale - scale) < 0.01f) return@Slider
                    onScaleChange(newScale)
                },
                valueRange = 0f..(UI_SCALES.size - 1).toFloat(),
                steps = UI_SCALES.size - 2
            )
        }
    }
}

@Composable
private fun RoundScreenSafePaddingSelector(
    scale: Float,
    onScaleChange: (Float) -> Unit
) {
    val idx = remember(scale) {
        ROUND_SCREEN_SAFE_PADDING_SCALES.indexOfFirst { kotlin.math.abs(it - scale) < 0.01f }
            .takeIf { it >= 0 }
            ?: ROUND_SCREEN_SAFE_PADDING_SCALES.indexOf(1.0f).coerceAtLeast(0)
    }
    var sliderIdx by remember(scale) { mutableIntStateOf(idx) }
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("圆屏安全边距", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "仅圆形屏幕生效，0% 为关闭，100% 为默认",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(ROUND_SCREEN_SAFE_PADDING_SCALES[sliderIdx] * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderIdx.toFloat(),
                onValueChange = {
                    sliderIdx = sliderIndex(it, ROUND_SCREEN_SAFE_PADDING_SCALES.lastIndex)
                },
                onValueChangeFinished = {
                    val newScale = ROUND_SCREEN_SAFE_PADDING_SCALES[sliderIdx]
                    if (kotlin.math.abs(newScale - scale) < 0.01f) return@Slider
                    onScaleChange(newScale)
                },
                valueRange = 0f..(ROUND_SCREEN_SAFE_PADDING_SCALES.size - 1).toFloat(),
                steps = ROUND_SCREEN_SAFE_PADDING_SCALES.size - 2
            )
        }
    }
}

@Composable
private fun PullRefreshDistanceSelector(
    distanceDp: Float,
    onDistanceChange: (Float) -> Unit
) {
    val idx = remember(distanceDp) {
        PULL_REFRESH_DISTANCES.indexOfFirst { kotlin.math.abs(it - distanceDp) < 0.01f }
            .takeIf { it >= 0 }
            ?: PULL_REFRESH_DISTANCES.indexOf(DEFAULT_PULL_REFRESH_DISTANCE_DP).coerceAtLeast(0)
    }
    var sliderIdx by remember(distanceDp) { mutableIntStateOf(idx) }
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("下拉刷新行程", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "数值越大，下拉越长才会触发刷新",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${PULL_REFRESH_DISTANCES[sliderIdx].toInt()}dp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Slider(
                value = sliderIdx.toFloat(),
                onValueChange = {
                    sliderIdx = sliderIndex(it, PULL_REFRESH_DISTANCES.lastIndex)
                },
                onValueChangeFinished = {
                    val newDistance = PULL_REFRESH_DISTANCES[sliderIdx]
                    if (kotlin.math.abs(newDistance - distanceDp) < 0.01f) return@Slider
                    onDistanceChange(newDistance)
                },
                valueRange = 0f..(PULL_REFRESH_DISTANCES.size - 1).toFloat(),
                steps = PULL_REFRESH_DISTANCES.size - 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("短", style = MaterialTheme.typography.bodySmall)
                Text("标准", style = MaterialTheme.typography.bodySmall)
                Text("长", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TransitionStyleSelector(
    selected: TransitionStyle,
    onSelect: (TransitionStyle) -> Unit
) {
    SettingDropdown(
        title = "过渡动画",
        selected = selected,
        options = TransitionStyle.entries,
        optionLabel = ::transitionStyleLabel,
        onSelect = onSelect
    )
}

private fun transitionStyleLabel(style: TransitionStyle): String = when (style) {
    TransitionStyle.NONE -> "无动画"
    TransitionStyle.SHARED_AXIS_X -> "水平滑动"
    TransitionStyle.SHARED_AXIS_Y -> "垂直滑动"
    TransitionStyle.SHARED_AXIS_Z -> "缩放"
    TransitionStyle.FADE_THROUGH -> "淡入淡出"
}

@Composable
private fun AnimationSpeedSelector(
    speed: AnimationSpeed,
    onSelect: (AnimationSpeed) -> Unit
) {
    SettingDropdown(
        title = "动画速度",
        selected = speed,
        options = AnimationSpeed.entries,
        optionLabel = ::animationSpeedLabel,
        onSelect = onSelect
    )
}

private fun animationSpeedLabel(speed: AnimationSpeed): String = when (speed) {
    AnimationSpeed.OFF -> "关闭"
    AnimationSpeed.FAST -> "快速"
    AnimationSpeed.NORMAL -> "标准"
    AnimationSpeed.SLOW -> "慢速"
}

@Composable
private fun CornerStyleSelector(
    selected: CornerStyle,
    onSelect: (CornerStyle) -> Unit
) {
    SettingDropdown(
        title = "圆角风格",
        selected = selected,
        options = CornerStyle.entries,
        optionLabel = ::cornerStyleLabel,
        onSelect = onSelect
    )
}

private fun cornerStyleLabel(style: CornerStyle): String = when (style) {
    CornerStyle.SQUARE -> "直角"
    CornerStyle.STANDARD -> "标准"
    CornerStyle.ROUNDED -> "圆润"
    CornerStyle.CIRCULAR -> "圆形"
}
