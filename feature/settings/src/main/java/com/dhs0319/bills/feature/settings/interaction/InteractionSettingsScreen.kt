package com.dhs0319.bills.feature.settings.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.model.PlayerInteractionPrefs
import com.dhs0319.bills.core.model.playbackSpeedOptions
import com.dhs0319.bills.feature.settings.SettingsViewModel
import com.dhs0319.bills.feature.settings.components.SettingSwitch
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val playerSettings by viewModel.playerSettings.collectAsStateWithLifecycle()

    CollapsingTopBarScaffold(
        topBar = { scrollBehavior ->
            TopAppBar(
                title = { Text("交互设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SettingSwitch(
                title = "双击暂停/继续",
                subtitle = "双击播放器中间区域切换播放状态",
                checked = playerSettings.interaction.doubleTapPlayPause,
                onCheckedChange = viewModel::updateDoubleTapPlayPause
            )

            SettingSwitch(
                title = "双击快进/后退",
                subtitle = "双击播放器右侧快进，左侧后退",
                checked = playerSettings.interaction.doubleTapSeek,
                onCheckedChange = viewModel::updateDoubleTapSeek
            )

            SeekSecondsSetting(
                seconds = playerSettings.interaction.doubleTapSeekSeconds,
                onSecondsChange = viewModel::updateDoubleTapSeekSeconds
            )

            SettingSwitch(
                title = "长按倍速播放",
                subtitle = "长按屏幕时临时加速播放，松手恢复",
                checked = playerSettings.interaction.longPressSpeedEnabled,
                onCheckedChange = viewModel::updateLongPressSpeedEnabled
            )

            LongPressSpeedSetting(
                speed = playerSettings.playback.gestureSpeed,
                enabled = playerSettings.interaction.longPressSpeedEnabled,
                onSpeedChange = viewModel::updateGestureSpeed
            )

            SettingSwitch(
                title = "亮度控制手势",
                subtitle = "在播放器左侧上下滑动调节亮度",
                checked = playerSettings.interaction.brightnessGestureEnabled,
                onCheckedChange = viewModel::updateBrightnessGestureEnabled
            )

            SettingSwitch(
                title = "音量控制手势",
                subtitle = "在播放器右侧上下滑动调节音量",
                checked = playerSettings.interaction.volumeGestureEnabled,
                onCheckedChange = viewModel::updateVolumeGestureEnabled
            )
        }
    }
}

@Composable
private fun SeekSecondsSetting(
    seconds: Int,
    onSecondsChange: (Int) -> Unit
) {
    var sliderSeconds by remember(seconds) { mutableIntStateOf(seconds) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("快进和后退秒数", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${sliderSeconds}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "左右两侧双击均使用此时长",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = sliderSeconds.toFloat(),
                onValueChange = { sliderSeconds = it.roundToInt() },
                onValueChangeFinished = { onSecondsChange(sliderSeconds) },
                valueRange = PlayerInteractionPrefs.MIN_SEEK_SECONDS.toFloat()..
                    PlayerInteractionPrefs.MAX_SEEK_SECONDS.toFloat(),
                steps = PlayerInteractionPrefs.MAX_SEEK_SECONDS -
                    PlayerInteractionPrefs.MIN_SEEK_SECONDS - 1,
                modifier = Modifier.semantics { contentDescription = "快进和后退秒数" }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("5s", style = MaterialTheme.typography.bodySmall)
                Text("30s", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LongPressSpeedSetting(
    speed: Float,
    enabled: Boolean,
    onSpeedChange: (Float) -> Unit
) {
    val selectedIndex = playbackSpeedOptions.indices.minByOrNull { index ->
        abs(playbackSpeedOptions[index] - speed)
    } ?: 0
    var dragIndex by remember { mutableStateOf<Int?>(null) }
    val displayIndex = dragIndex ?: selectedIndex

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("长按播放倍速", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${playbackSpeedOptions[displayIndex]}x",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                "长按时使用的临时倍速",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = displayIndex.toFloat(),
                onValueChange = { dragIndex = it.roundToInt().coerceIn(0, playbackSpeedOptions.lastIndex) },
                onValueChangeFinished = {
                    dragIndex?.let { onSpeedChange(playbackSpeedOptions[it]) }
                    dragIndex = null
                },
                valueRange = 0f..playbackSpeedOptions.lastIndex.toFloat(),
                steps = playbackSpeedOptions.size - 2,
                enabled = enabled,
                modifier = Modifier.semantics { contentDescription = "长按播放倍速" }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0.25x", style = MaterialTheme.typography.bodySmall)
                Text("3.0x", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
