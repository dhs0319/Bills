package com.dhs0319.bills.feature.download.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.dhs0319.bills.core.designsystem.component.DanmakuSettingsSection
import com.dhs0319.bills.core.designsystem.component.PlaybackOption
import com.dhs0319.bills.core.designsystem.component.PlaybackOptionBottomSheet
import com.dhs0319.bills.core.designsystem.component.PlaybackOptionSidebar
import com.dhs0319.bills.core.designsystem.component.PlaybackSettingsPanel
import com.dhs0319.bills.core.designsystem.component.shouldShowPlaybackSidebar
import com.dhs0319.bills.core.designsystem.component.PlayerBottomControls
import com.dhs0319.bills.core.designsystem.component.PlayerControlTextButton
import com.dhs0319.bills.infra.player.danmaku.DanmakuLayer
import com.dhs0319.bills.infra.player.danmaku.rememberDanmakuOverlayState
import com.dhs0319.bills.core.model.DanmakuConfig
import com.dhs0319.bills.core.model.PlayerSettingsState
import kotlinx.coroutines.delay
import java.util.Locale

private val downloadSpeedOptions = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 3f)

@Suppress("UnsafeOptInUsageError")
@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DownloadPlayerPane(
    modifier: Modifier,
    viewModel: DownloadPlayerViewModel,
    isFull: Boolean,
    onToggleFull: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle(initialValue = PlayerSettingsState())
    val danmakuState by viewModel.danmakuState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val useSidebar = shouldShowPlaybackSidebar(isFull)
    val tapSource = remember { MutableInteractionSource() }
    var showControls by remember { mutableStateOf(true) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var dragPositionMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(showControls, state.isPlaying, dragPositionMs, showSpeedDialog, showSettingsSheet) {
        if (showControls && state.isPlaying && dragPositionMs == null && !showSpeedDialog && !showSettingsSheet) {
            delay(3_000)
            showControls = false
        }
    }

    val durationMs = player?.duration?.takeIf { it > 0L } ?: state.durationMs.coerceAtLeast(0L)
    val barPositionMs = dragPositionMs ?: state.positionMs.coerceAtLeast(0L)
    val sliderValue = if (durationMs > 0L) {
        (barPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val danmakuConfig = settingsState.danmaku
    val playerView = remember(context) {
        PlayerView(context).apply {
            useController = false
            setKeepContentOnPlayerReset(true)
            setEnableComposeSurfaceSyncWorkaround(true)
        }
    }
    val danmakuOverlayState = if (danmakuConfig.enabled) {
        rememberDanmakuOverlayState(
            initialConfig = danmakuConfig,
            initialPositionMs = state.positionMs,
            initialIsPlaying = state.isPlaying,
            initialSpeed = state.speed
        )
    } else {
        null
    }

    DisposableEffect(playerView) {
        onDispose {
            playerView.player = null
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { playerView },
            update = { view ->
                if (view.player !== player) {
                    view.player = player
                }
                view.keepScreenOn = state.playWhenReady
            },
            modifier = Modifier.fillMaxSize()
        )

        if (danmakuOverlayState != null) {
            DanmakuLayer(
                playerView = playerView,
                overlayState = danmakuOverlayState,
                danmakuState = danmakuState,
                danmakuConfig = danmakuConfig,
                positionMs = state.positionMs,
                isPlaying = state.isPlaying,
                speed = state.speed,
                seekEventId = state.seekEventId,
                hasSource = state.taskId != null
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = tapSource,
                    indication = null
                ) {
                    showControls = !showControls
                }
        )

        if (showControls) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 12.dp,
                        top = 12.dp,
                        end = if (isFull) 20.dp else 10.dp,
                        bottom = 12.dp
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = Color.White
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isFull) 0.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            showControls = true
                            viewModel.updateDanmaku(
                                danmakuConfig.copy(enabled = !danmakuConfig.enabled)
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = if (danmakuConfig.enabled) {
                                Icons.Default.Subtitles
                            } else {
                                Icons.Default.SubtitlesOff
                            },
                            contentDescription = if (danmakuConfig.enabled) "关闭弹幕" else "开启弹幕",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = {
                            showControls = true
                            showSettingsSheet = true
                            showSpeedDialog = false
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多设置",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        if (showControls) {
            PlayerBottomControls(
                isPlaying = state.isPlaying,
                playContentDescription = if (state.isPlaying) "暂停" else "播放",
                timeText = formatDownloadPlaybackTime(barPositionMs, durationMs),
                progressValue = sliderValue,
                progressEnabled = durationMs > 0L,
                isFullscreen = isFull,
                fullscreenContentDescription = if (isFull) "还原" else "全屏",
                onTogglePlay = viewModel::togglePlayPause,
                onToggleFullscreen = {
                    showControls = true
                    onToggleFull()
                },
                onProgressChange = { fraction ->
                    showControls = true
                    dragPositionMs = (durationMs * fraction).toLong()
                },
                onProgressChangeFinished = {
                    val next = dragPositionMs ?: return@PlayerBottomControls
                    viewModel.seekTo(next)
                    dragPositionMs = null
                },
                modifier = Modifier.align(Alignment.BottomCenter),
                optionActions = {
                    PlayerControlTextButton(
                        label = "倍速",
                        text = formatDownloadSpeed(state.speed),
                        onClick = {
                            showControls = true
                            showSpeedDialog = true
                            showSettingsSheet = false
                        }
                    )
                }
            )
        }

        if (showSettingsSheet && useSidebar) {
            DownloadPlayerSettingsSheet(
                settingsState = settingsState,
                onDismiss = { showSettingsSheet = false },
                onDanmakuConfigChange = viewModel::updateDanmaku,
                onBackgroundPlaybackChange = viewModel::updateBackgroundPlayback,
                embedded = true
            )
        }

        if (showSpeedDialog && useSidebar) {
            PlaybackOptionSidebar(
                embedded = true,
                title = "播放速度",
                options = downloadSpeedOptions.map { speed ->
                    PlaybackOption(
                        id = speed.toString(),
                        label = formatDownloadSpeed(speed),
                        selected = speed == state.speed
                    )
                },
                onDismiss = { showSpeedDialog = false },
                onSelect = { speed ->
                    viewModel.setSpeed(speed.toFloat())
                    showSpeedDialog = false
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showSpeedDialog && !useSidebar) {
        PlaybackOptionBottomSheet(
            title = "播放速度",
            options = downloadSpeedOptions.map { speed ->
                PlaybackOption(
                    id = speed.toString(),
                    label = formatDownloadSpeed(speed),
                    selected = speed == state.speed
                )
            },
            onDismiss = { showSpeedDialog = false },
            onSelect = { speed ->
                viewModel.setSpeed(speed.toFloat())
                showSpeedDialog = false
            }
        )
    }

    if (showSettingsSheet && !useSidebar) {
        DownloadPlayerSettingsSheet(
            settingsState = settingsState,
            onDismiss = { showSettingsSheet = false },
            onDanmakuConfigChange = viewModel::updateDanmaku,
            onBackgroundPlaybackChange = viewModel::updateBackgroundPlayback
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadPlayerSettingsSheet(
    settingsState: PlayerSettingsState,
    onDismiss: () -> Unit,
    onDanmakuConfigChange: (DanmakuConfig) -> Unit,
    onBackgroundPlaybackChange: (Boolean) -> Unit,
    embedded: Boolean = false
) {
    PlaybackSettingsPanel(title = "更多设置", onDismiss = onDismiss, embedded = embedded) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DownloadSwitchCard(
                title = "后台播放",
                subtitle = "退出页面或切到后台后继续播放",
                checked = settingsState.playback.backgroundPlayback,
                onCheckedChange = onBackgroundPlaybackChange
            )
            DanmakuSettingsSection(
                config = settingsState.danmaku,
                onConfigChange = onDanmakuConfigChange
            )
        }
    }
}

@Composable
private fun DownloadSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

internal fun formatDownloadPlaybackTime(
    positionMs: Long,
    durationMs: Long
): String {
    return "${formatDownloadDuration(positionMs)} / ${formatDownloadDuration(durationMs)}"
}

private fun formatDownloadDuration(valueMs: Long): String {
    val totalSec = (valueMs / 1_000L).coerceAtLeast(0L)
    val hour = totalSec / 3600L
    val minute = (totalSec % 3600L) / 60L
    val second = totalSec % 60L
    return if (hour > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hour, minute, second)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minute, second)
    }
}

internal fun formatDownloadSpeed(value: Float): String {
    val formatted = if (value % 1f == 0f) {
        String.format(Locale.ROOT, "%.0f", value)
    } else {
        String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
    }
    return "${formatted}x"
}
