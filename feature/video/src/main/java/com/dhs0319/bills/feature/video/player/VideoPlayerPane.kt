package com.dhs0319.bills.feature.video.player

import android.media.AudioManager
import android.os.BatteryManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.R as Media3UiR
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.dhs0319.bills.core.designsystem.component.PlaybackBufferingOverlay
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.PlaybackOption
import com.dhs0319.bills.core.designsystem.component.PlaybackOptionBottomSheet
import com.dhs0319.bills.core.designsystem.component.PlaybackOptionSidebar
import com.dhs0319.bills.core.designsystem.component.shouldShowPlaybackSidebar
import com.dhs0319.bills.core.designsystem.component.PlayerBottomControls
import com.dhs0319.bills.core.designsystem.component.PlayerControlTextButton
import com.dhs0319.bills.core.designsystem.component.formatPlaybackDownloadSpeed
import com.dhs0319.bills.infra.player.danmaku.DanmakuLayer
import com.dhs0319.bills.infra.player.danmaku.DanmakuOverlayState
import com.dhs0319.bills.infra.player.danmaku.rememberDanmakuOverlayState
import com.dhs0319.bills.infra.player.PlayerViewTargetBinder
import com.dhs0319.bills.core.model.PlaybackState
import com.dhs0319.bills.core.model.PlayerSettingsState
import com.dhs0319.bills.core.model.VideoPlaybackState
import com.dhs0319.bills.feature.video.VideoViewModel
import com.dhs0319.bills.feature.video.formatPlaybackTime
import com.dhs0319.bills.feature.video.formatSpeed
import com.dhs0319.bills.feature.video.getAudioName
import com.dhs0319.bills.feature.video.getQualityName
import com.dhs0319.bills.feature.video.speedOps
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToInt

internal enum class PlayerVideoResizeMode {
    Fit,
    Zoom,
    Fill
}

private enum class PlayerDialog {
    Quality,
    Audio,
    Speed
}

private data class PlayerOptionPanelState(
    val title: String,
    val options: List<PlaybackOption>,
    val onSelect: (String) -> Unit
)

internal val LocalVideoResizeModeState = compositionLocalOf<MutableState<PlayerVideoResizeMode>> {
    error("Missing player video resize mode state")
}

@Suppress("UnsafeOptInUsageError")
@UnstableApi
@Composable
internal fun VideoPlayerPane(
    modifier: Modifier,
    viewModel: VideoViewModel,
    videoTitle: String?,
    isFull: Boolean,
    onToggleFull: () -> Unit,
    onBackClick: () -> Unit,
    onGoHome: () -> Unit
) {
    val context = LocalContext.current
    val useSidebar = shouldShowPlaybackSidebar(isFull)
    val owner = LocalLifecycleOwner.current
    val timeFmt = remember { android.text.format.DateFormat.getTimeFormat(context) }
    val state by viewModel.videoState.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val downloadSpeedBytesPerSecond by viewModel.downloadSpeedBytesPerSecond.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsState.collectAsStateWithLifecycle(initialValue = PlayerSettingsState())
    var activeDialog by remember { mutableStateOf<PlayerDialog?>(null) }
    var showPlaybackSheet by remember { mutableStateOf(false) }
    var showCtrl by remember { mutableStateOf(false) }
    val replayInteractionSource = remember { MutableInteractionSource() }
    val videoResizeMode = rememberSaveable { mutableStateOf(PlayerVideoResizeMode.Fit) }
    val activeOptionPanel = when (activeDialog) {
        PlayerDialog.Quality -> state.playbackSource?.let { source ->
            PlayerOptionPanelState(
                title = "选择画质",
                options = source.qualityOptions.map { option ->
                    PlaybackOption(
                        id = option.quality.toString(),
                        label = option.description,
                        selected = option.quality == state.currentStream?.quality,
                        needVip = option.needVip,
                        supportingText = option.limit?.message
                    )
                },
                onSelect = { quality ->
                    viewModel.switchQuality(quality.toInt())
                    activeDialog = null
                }
            )
        }

        PlayerDialog.Audio -> state.playbackSource?.let { source ->
            PlayerOptionPanelState(
                title = "选择音频",
                options = source.audios.map { audio ->
                    PlaybackOption(
                        id = audio.id.toString(),
                        label = getAudioName(audio.id),
                        selected = audio.id == state.currentAudio?.id
                    )
                },
                onSelect = { audioId ->
                    viewModel.switchAudio(audioId.toInt())
                    activeDialog = null
                }
            )
        }

        PlayerDialog.Speed -> PlayerOptionPanelState(
            title = "播放速度",
            options = speedOps.map { speed ->
                PlaybackOption(
                    id = speed.toString(),
                    label = formatSpeed(speed),
                    selected = speed == state.speed
                )
            },
            onSelect = { speed ->
                viewModel.setSpeed(speed.toFloat())
                activeDialog = null
            }
        )

        null -> null
    }
    val topStatus = remember(showCtrl, isFull, downloadSpeedBytesPerSecond, settingsState.overlay) {
        if (showCtrl && isFull) {
            readPlayerTopStatus(
                context = context,
                timeFmt = timeFmt,
                downloadSpeedBytesPerSecond = downloadSpeedBytesPerSecond,
                showTime = settingsState.overlay.showTime,
                showNetworkSpeed = settingsState.overlay.showNetworkSpeed,
                showBattery = settingsState.overlay.showBattery
            )
        } else {
            null
        }
    }
    val danmakuOn = settingsState.danmaku.enabled
    val danmakuOverlayState = if (danmakuOn && !state.waitingForPlay) {
        rememberDanmakuOverlayState(
            initialConfig = settingsState.danmaku,
            initialPositionMs = viewModel.playbackProgress.value.positionMs,
            initialIsPlaying = state.isPlaying,
            initialSpeed = state.speed
        )
    } else {
        null
    }
    val videoAspect = remember(state.currentStream) {
        val width = state.currentStream?.width?.takeIf { it > 0 } ?: return@remember null
        val height = state.currentStream?.height?.takeIf { it > 0 } ?: return@remember null
        width.toFloat() / height.toFloat()
    }
    val resizeMode = remember(isFull, videoResizeMode.value) {
        if (!isFull) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT
        } else {
            when (videoResizeMode.value) {
                PlayerVideoResizeMode.Fit -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                PlayerVideoResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                PlayerVideoResizeMode.Fill -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            }
        }
    }
    val playerView = remember(context) {
        PlayerView(context).apply {
            useController = false
            setEnableComposeSurfaceSyncWorkaround(true)
            setKeepContentOnPlayerReset(true)
        }
    }
    var lastWarmAspect by remember(playerView) { mutableStateOf<Float?>(null) }

    LaunchedEffect(state.playWhenReady) {
        playerView.keepScreenOn = state.playWhenReady
    }

    LaunchedEffect(state.waitingForPlay) {
        if (state.waitingForPlay) {
            showCtrl = false
            activeDialog = null
            showPlaybackSheet = false
        }
    }

    DisposableEffect(owner, playerView, player, state.waitingForPlay) {
        val lifecycle = owner.lifecycle
        if (!state.waitingForPlay && lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            PlayerViewTargetBinder.bind(playerView, player)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!state.waitingForPlay) {
                    PlayerViewTargetBinder.bind(playerView, player)
                }
                Lifecycle.Event.ON_STOP -> PlayerViewTargetBinder.unbind(playerView)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            PlayerViewTargetBinder.unbind(playerView)
        }
    }

    CompositionLocalProvider(LocalVideoResizeModeState provides videoResizeMode) {
        Box(
            modifier = modifier
                .background(Color.Black)
        ) {
            if (!state.waitingForPlay) {
                AndroidView(
                    factory = { playerView },
                    update = { view ->
                        val content = view.findViewById<AspectRatioFrameLayout>(Media3UiR.id.exo_content_frame)
                        if (content != null) {
                            content.resizeMode = resizeMode
                            if (lastWarmAspect != videoAspect) {
                                content.setAspectRatio(videoAspect ?: 0f)
                                lastWarmAspect = videoAspect
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (
                state.waitingForPlay ||
                !state.hasRenderedFirstFrame &&
                !state.detail?.cover.isNullOrBlank() &&
                (state.isPreparing || state.playbackSource != null)
            ) {
                val onPlayVideo: () -> Unit = {
                    showCtrl = false
                    activeDialog = null
                    showPlaybackSheet = false
                    viewModel.resume()
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .then(
                            if (state.waitingForPlay) {
                                Modifier.clickable(onClickLabel = "播放视频", onClick = onPlayVideo)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    BiliAsyncImage(
                        url = state.detail?.cover,
                        contentDescription = state.detail?.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (state.waitingForPlay) {
                        IconButton(
                            onClick = onPlayVideo,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "播放视频",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            PlaybackBufferingOverlay(
                visible = !state.waitingForPlay && state.playerError.isNullOrBlank() &&
                    (state.isPreparing || state.playbackState == PlaybackState.Buffering),
                modifier = Modifier.align(Alignment.Center)
            )

            if (danmakuOverlayState != null) {
                VideoDanmakuLayer(
                    viewModel = viewModel,
                    state = state,
                    settingsState = settingsState,
                    danmakuOverlayState = danmakuOverlayState,
                    playerView = playerView
                )
            }

            if (!state.waitingForPlay) {
                VideoPlayerOverlay(
                    viewModel = viewModel,
                    state = state,
                    player = player,
                    settingsState = settingsState,
                    isFull = isFull,
                    showCtrl = showCtrl,
                    activeDialog = activeDialog,
                    showPlaybackSheet = showPlaybackSheet,
                    onShowCtrlChange = { showCtrl = it },
                    onShowA = {
                        showCtrl = true
                        activeDialog = PlayerDialog.Audio
                        showPlaybackSheet = false
                    },
                    onShowQ = {
                        showCtrl = true
                        activeDialog = PlayerDialog.Quality
                        showPlaybackSheet = false
                    },
                    onShowSp = {
                        showCtrl = true
                        activeDialog = PlayerDialog.Speed
                        showPlaybackSheet = false
                    },
                    onToggleFull = {
                        showCtrl = true
                        onToggleFull()
                    }
                )
            }

            if (showCtrl && !state.waitingForPlay) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .heightIn(min = 82.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.54f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    if (isFull) {
                        topStatus?.let { status ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 20.dp)
                                    .padding(start = 32.dp, end = 28.dp)
                            ) {
                                status.time?.let { time ->
                                    Text(
                                        text = time,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                                Row(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    status.downloadSpeedText?.let { speedText ->
                                        Text(
                                            text = speedText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                    }
                                    status.batteryPercent?.let { batteryPercent ->
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$batteryPercent%",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.9f)
                                            )
                                            BatteryLevelIcon(batteryPercent = batteryPercent)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 10.dp,
                                end = if (isFull) 20.dp else 10.dp
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
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
                            if (!isFull) {
                                IconButton(
                                    onClick = onGoHome,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = "首页",
                                        tint = Color.White
                                    )
                                }
                            } else {
                                videoTitle?.takeIf(String::isNotBlank)?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        softWrap = false,
                                        modifier = Modifier
                                            .widthIn(max = LocalConfiguration.current.screenWidthDp.dp / 2)
                                            .basicMarquee(iterations = Int.MAX_VALUE)
                                    )
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                if (isFull) 0.dp else 8.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isFull) {
                                val currentResizeMode = videoResizeMode.value
                                val nextResizeMode = when (currentResizeMode) {
                                    PlayerVideoResizeMode.Fit -> PlayerVideoResizeMode.Zoom
                                    PlayerVideoResizeMode.Zoom -> PlayerVideoResizeMode.Fill
                                    PlayerVideoResizeMode.Fill -> PlayerVideoResizeMode.Fit
                                }
                                IconButton(
                                    onClick = {
                                        showCtrl = true
                                        videoResizeMode.value = nextResizeMode
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = when (currentResizeMode) {
                                            PlayerVideoResizeMode.Fit -> Icons.Default.AspectRatio
                                            PlayerVideoResizeMode.Zoom -> Icons.Default.CropFree
                                            PlayerVideoResizeMode.Fill -> Icons.Default.OpenInFull
                                        },
                                        contentDescription = "画面尺寸：${videoResizeModeText(currentResizeMode)}，点击切换为${videoResizeModeText(nextResizeMode)}",
                                        tint = Color.White
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    showCtrl = true
                                    viewModel.updateDanmaku(
                                        settingsState.danmaku.copy(enabled = !settingsState.danmaku.enabled)
                                    )
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = if (danmakuOn) {
                                        Icons.Default.Subtitles
                                    } else {
                                        Icons.Default.SubtitlesOff
                                    },
                                    contentDescription = if (danmakuOn) "关闭弹幕" else "开启弹幕",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = {
                                    showCtrl = true
                                    showPlaybackSheet = true
                                    activeDialog = null
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "更多信息",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (useSidebar && showPlaybackSheet && !state.waitingForPlay) {
                VideoPlaybackSidebar(
                    embedded = true,
                    state = state,
                    viewModel = viewModel,
                    onDismiss = { showPlaybackSheet = false },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxSize()
                    )
            }

            if (useSidebar && !state.waitingForPlay) {
                activeOptionPanel?.let { panel ->
                    PlaybackOptionSidebar(
                        embedded = true,
                        title = panel.title,
                        options = panel.options,
                        onDismiss = { activeDialog = null },
                        onSelect = panel.onSelect,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxSize()
                    )
                }
            }

            if (!state.waitingForPlay && state.playbackState == PlaybackState.Ended && state.playerError.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f))
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable(
                                interactionSource = replayInteractionSource,
                                indication = null
                            ) {
                                activeDialog = null
                                showPlaybackSheet = false
                                showCtrl = false
                                viewModel.replay()
                            }
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay,
                            contentDescription = "重播",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "重播",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }

        if (!useSidebar && !state.waitingForPlay) {
            activeOptionPanel?.let { panel ->
                PlaybackOptionBottomSheet(
                    title = panel.title,
                    options = panel.options,
                    onDismiss = { activeDialog = null },
                    onSelect = panel.onSelect
                )
            }
        }

        if (!useSidebar && showPlaybackSheet && !state.waitingForPlay) {
            VideoPlaybackSheet(
                state = state,
                viewModel = viewModel,
                onDismiss = { showPlaybackSheet = false }
            )
        }
    }
}

@UnstableApi
@Composable
private fun VideoDanmakuLayer(
    viewModel: VideoViewModel,
    state: VideoPlaybackState,
    settingsState: PlayerSettingsState,
    danmakuOverlayState: DanmakuOverlayState,
    playerView: PlayerView
) {
    val danmakuState by viewModel.danmakuState.collectAsStateWithLifecycle()

    DanmakuLayer(
        playerView = playerView,
        overlayState = danmakuOverlayState,
        danmakuState = danmakuState,
        danmakuConfig = settingsState.danmaku,
        positionMs = viewModel.playbackProgress.value.positionMs,
        isPlaying = state.isPlaying,
        speed = state.speed,
        seekEventId = state.seekEventId,
        hasSource = state.playbackSource != null
    )
}

@Composable
private fun VideoPlayerOverlay(
    viewModel: VideoViewModel,
    state: VideoPlaybackState,
    player: Player?,
    settingsState: PlayerSettingsState,
    isFull: Boolean,
    showCtrl: Boolean,
    activeDialog: PlayerDialog?,
    showPlaybackSheet: Boolean,
    onShowCtrlChange: (Boolean) -> Unit,
    onShowA: () -> Unit,
    onShowQ: () -> Unit,
    onShowSp: () -> Unit,
    onToggleFull: () -> Unit
) {
    val context = LocalContext.current
    val gestureState = remember { VideoGestureState() }
    var dragMs by remember { mutableStateOf<Long?>(null) }
    var dragStartBrightness by remember { mutableFloatStateOf(0.5f) }
    var dragStartVolumeFrac by remember { mutableFloatStateOf(0f) }
    var speedBeforeGesture by remember { mutableFloatStateOf(1f) }
    var lastGestureBrightness by remember { mutableFloatStateOf(Float.NaN) }
    var lastGestureVolumeFrac by remember { mutableFloatStateOf(Float.NaN) }
    val audioManager = remember(context) { context.getSystemService(AudioManager::class.java) }
    val act = LocalActivity.current

    LaunchedEffect(
        showCtrl,
        state.isPlaying,
        dragMs,
        gestureState.dragType,
        gestureState.showSpeedBadge,
        activeDialog,
        showPlaybackSheet
    ) {
        if (
            showCtrl &&
            state.isPlaying &&
            dragMs == null &&
            gestureState.dragType == DragType.None &&
            !gestureState.showSpeedBadge &&
            activeDialog == null &&
            !showPlaybackSheet
        ) {
            delay(2_000)
            onShowCtrlChange(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .videoGestures(
                    state = gestureState,
                    onToggleControls = { onShowCtrlChange(!showCtrl) },
                    onTogglePlay = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo,
                    onStartSpeedUp = {
                        speedBeforeGesture = state.speed
                        val speed = settingsState.playback.gestureSpeed
                        viewModel.setSpeed(speed)
                        formatSpeed(speed)
                    },
                    onStopSpeedUp = { viewModel.setSpeed(speedBeforeGesture) },
                    onBrightnessDelta = { deltaFrac ->
                        val next = (dragStartBrightness + deltaFrac).coerceIn(0.01f, 1f)
                        if (abs(next - lastGestureBrightness) >= 0.02f || lastGestureBrightness.isNaN()) {
                            adjustWindowBrightness(act, next)
                            lastGestureBrightness = next
                        }
                        next
                    },
                    onVolumeDelta = { deltaFrac ->
                        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                        if (maxVol <= 0) {
                            0f
                        } else {
                            val next = (dragStartVolumeFrac + deltaFrac).coerceIn(0f, 1f)
                            if (abs(next - lastGestureVolumeFrac) >= (1f / maxVol.toFloat()) || lastGestureVolumeFrac.isNaN()) {
                                val newVol = (next * maxVol).roundToInt()
                                adjustStreamVolume(audioManager, newVol)
                                lastGestureVolumeFrac = next
                            }
                            next
                        }
                    },
                    onDragStart = { dragType ->
                        when (dragType) {
                            DragType.Brightness -> {
                                dragStartBrightness = act?.window?.attributes?.screenBrightness
                                    ?.takeIf { it in 0f..1f }
                                    ?.coerceIn(0.01f, 1f)
                                    ?: 0.5f
                                lastGestureBrightness = Float.NaN
                            }
                            DragType.Volume -> {
                                val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
                                val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                                dragStartVolumeFrac = if (maxVol > 0) {
                                    curVol.toFloat() / maxVol.toFloat()
                                } else {
                                    0f
                                }
                                lastGestureVolumeFrac = Float.NaN
                            }
                            else -> Unit
                        }
                    },
                    isPlaying = { state.isPlaying },
                    positionMs = {
                        val progress = viewModel.playbackProgress.value
                        dragMs
                            ?: gestureState.dragSeekPosMs
                            ?: progress.positionMs
                    },
                    durationMs = {
                        val progress = viewModel.playbackProgress.value
                        player?.duration
                            ?.takeIf { it > 0L }
                            ?: progress.durationMs.takeIf { it > 0L }
                            ?: state.playbackSource?.durationMs?.coerceAtLeast(0L)
                            ?: 0L
                    }
                )
        )

        val gestureDurationMs = player?.duration
            ?.takeIf { it > 0L }
            ?: viewModel.playbackProgress.value.durationMs
                .takeIf { it > 0L }
            ?: state.playbackSource?.durationMs?.coerceAtLeast(0L)
            ?: 0L
        VideoGestureFeedback(
            state = gestureState,
            durationMs = gestureDurationMs
        )

        if (showCtrl) {
            PlayerCtrlBarHost(
                viewModel = viewModel,
                state = state,
                player = player,
                isFull = isFull,
                dragMs = { dragMs },
                gestureSeekMs = gestureState.dragSeekPosMs,
                onDragMsChange = { dragMs = it },
                onShowCtrlChange = onShowCtrlChange,
                onShowA = onShowA,
                onShowQ = onShowQ,
                onShowSp = onShowSp,
                onToggleFull = onToggleFull,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun PlayerCtrlBarHost(
    viewModel: VideoViewModel,
    state: VideoPlaybackState,
    player: Player?,
    isFull: Boolean,
    dragMs: () -> Long?,
    gestureSeekMs: Long?,
    onDragMsChange: (Long?) -> Unit,
    onShowCtrlChange: (Boolean) -> Unit,
    onShowA: () -> Unit,
    onShowQ: () -> Unit,
    onShowSp: () -> Unit,
    onToggleFull: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val durationMs = player?.duration
        ?.takeIf { it > 0L }
        ?: progress.durationMs.takeIf { it > 0L }
        ?: state.playbackSource?.durationMs?.coerceAtLeast(0L)
        ?: 0L
    val dragSeekMs = dragMs()
    val barMs = (dragSeekMs ?: gestureSeekMs ?: progress.positionMs)
        .coerceIn(0L, durationMs)
    val sliderVal = if (durationMs > 0L) {
        (barMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val bufferedSliderVal = if (durationMs > 0L) {
        val bufferedPositionMs = player?.bufferedPosition
            ?.takeIf { it > 0L }
            ?: progress.bufferedPositionMs
        (bufferedPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    PlayerBottomControls(
        isPlaying = state.isPlaying,
        playContentDescription = if (state.isPlaying) "暂停" else "播放",
        timeText = formatPlaybackTime(barMs, durationMs),
        progressValue = sliderVal,
        bufferedProgressValue = bufferedSliderVal,
        progressEnabled = durationMs > 0L,
        isFullscreen = isFull,
        fullscreenContentDescription = if (isFull) "还原" else "全屏",
        onTogglePlay = viewModel::togglePlayPause,
        onToggleFullscreen = onToggleFull,
        onProgressChange = { frac ->
            onShowCtrlChange(true)
            onDragMsChange((durationMs * frac).toLong())
        },
        onProgressChangeFinished = {
            val next = dragMs() ?: return@PlayerBottomControls
            viewModel.seekTo(next)
            onDragMsChange(null)
        },
        modifier = modifier,
        optionActions = {
            if ((state.playbackSource?.audios?.size ?: 0) > 1) {
                PlayerControlTextButton(
                    label = "音频",
                    text = state.currentAudio?.let { getAudioName(it.id, short = true) } ?: "音频",
                    onClick = onShowA
                )
            }
            PlayerControlTextButton(
                label = "画质",
                text = getQualityName(state.playbackSource, state.currentStream),
                enabled = (state.playbackSource?.qualityOptions?.size ?: 0) > 1,
                onClick = onShowQ
            )
            PlayerControlTextButton(
                label = "倍速",
                text = formatSpeed(state.speed),
                onClick = onShowSp
            )
        }
    )
}

private data class PlayerTopStatus(
    val time: String?,
    val batteryPercent: Int?,
    val downloadSpeedText: String?
)

@Composable
private fun BatteryLevelIcon(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    val level = batteryPercent.coerceIn(0, 100) / 100f
    val color = Color.White.copy(alpha = 0.9f)

    Canvas(modifier = modifier.size(width = 22.dp, height = 11.dp)) {
        val strokeWidth = 1.dp.toPx()
        val terminalGap = 1.dp.toPx()
        val terminalWidth = 2.dp.toPx()
        val bodyWidth = size.width - terminalGap - terminalWidth
        val innerInset = strokeWidth * 2f
        val innerWidth = (bodyWidth - innerInset * 2f).coerceAtLeast(0f)

        drawRoundRect(
            color = color,
            topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(bodyWidth - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )
        if (level > 0f) {
            drawRoundRect(
                color = color,
                topLeft = Offset(innerInset, innerInset),
                size = Size(innerWidth * level, size.height - innerInset * 2f),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(bodyWidth + terminalGap, size.height * 0.3f),
            size = Size(terminalWidth, size.height * 0.4f),
            cornerRadius = CornerRadius(terminalWidth / 2f)
        )
    }
}

private fun readPlayerTopStatus(
    context: android.content.Context,
    timeFmt: java.text.DateFormat,
    downloadSpeedBytesPerSecond: Long,
    showTime: Boolean,
    showNetworkSpeed: Boolean,
    showBattery: Boolean
): PlayerTopStatus {
    val time = timeFmt.format(System.currentTimeMillis()).takeIf { showTime }
    val battery = if (showBattery) {
        context.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }
    } else {
        null
    }
    return PlayerTopStatus(
        time = time,
        batteryPercent = battery,
        downloadSpeedText = formatPlaybackDownloadSpeed(downloadSpeedBytesPerSecond)
            .takeIf { showNetworkSpeed }
    )
}
