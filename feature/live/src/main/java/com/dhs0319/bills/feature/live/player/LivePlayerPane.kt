package com.dhs0319.bills.feature.live.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.SubtitlesOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.BiliImageVariant
import com.dhs0319.bills.core.designsystem.component.PlaybackBufferingOverlay
import com.dhs0319.bills.core.designsystem.component.PlaybackOption
import com.dhs0319.bills.core.designsystem.component.PlaybackOptionBottomSheet
import com.dhs0319.bills.core.designsystem.component.PlaybackOptionSidebar
import com.dhs0319.bills.core.designsystem.component.PlaybackSidePanel
import com.dhs0319.bills.core.designsystem.component.shouldShowPlaybackSidebar
import com.dhs0319.bills.core.designsystem.component.PlayerBottomControls
import com.dhs0319.bills.core.designsystem.component.PlayerControlTextButton
import com.dhs0319.bills.core.model.LivePlaybackViewState
import com.dhs0319.bills.core.model.LiveRoomMessage
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.LiveRoomSessionState
import com.dhs0319.bills.core.model.LiveStatus
import com.dhs0319.bills.core.model.PlaybackState
import com.dhs0319.bills.core.model.PlayerSettingsState
import com.dhs0319.bills.core.model.DanmakuItem
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.feature.live.toUiMessage
import com.dhs0319.bills.infra.player.PlayerViewTargetBinder
import com.dhs0319.bills.infra.player.danmaku.DanmakuLayer
import com.dhs0319.bills.infra.player.danmaku.DanmakuOverlayState
import com.dhs0319.bills.infra.player.danmaku.DanmakuRenderMode
import com.dhs0319.bills.infra.player.danmaku.rememberDanmakuOverlayState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Suppress("UnsafeOptInUsageError")
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
internal fun LivePlayerPane(
    route: LiveRoute?,
    player: Player?,
    playbackState: LivePlaybackViewState,
    roomSessionState: StateFlow<LiveRoomSessionState>,
    isFull: Boolean,
    onToggleFull: () -> Unit,
    onTogglePlay: () -> Unit,
    onToggleDanmaku: (Boolean) -> Unit,
    onRetry: () -> Unit,
    onSwitchQuality: (Int) -> Unit,
    settingsState: PlayerSettingsState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val useSidebar = shouldShowPlaybackSidebar(isFull)
    val owner = LocalLifecycleOwner.current
    val tapSrc = remember { MutableInteractionSource() }
    var showCtrl by remember { mutableStateOf(true) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    val qualityText = playbackState.playbackSource?.currentDescription
        ?.takeIf(String::isNotBlank)
        ?: "画质"
    val qualityOptions = playbackState.playbackSource?.let { source ->
        source.qualityOptions.map { option ->
            PlaybackOption(
                id = option.qn.toString(),
                label = option.description,
                selected = option.qn == source.currentQn
            )
        }
    }
    val danmakuOn = settingsState.danmaku.enabled
    val playerView = remember(context) {
        PlayerView(context).apply {
            useController = false
            setEnableComposeSurfaceSyncWorkaround(true)
            setKeepContentOnPlayerReset(true)
        }
    }
    val danmakuOverlayState = if (danmakuOn) {
        rememberDanmakuOverlayState(
            initialConfig = settingsState.danmaku,
            initialPositionMs = 0L,
            initialIsPlaying = playbackState.isPlaying,
            initialSpeed = 1f
        )
    } else {
        null
    }
    val liveDanmakuState = remember(playbackState.playbackSource?.roomId) {
        DanmakuSessionState(
            sourceKey = playbackState.playbackSource?.roomId?.takeIf { it > 0L }?.let { "live:$it" }
        )
    }
    val roomId = playbackState.playbackSource?.roomId ?: 0L

    LaunchedEffect(
        showCtrl,
        playbackState.isPlaying,
        playbackState.error,
        showQualityDialog,
        showInfoDialog
    ) {
        if (
            showCtrl &&
            playbackState.isPlaying &&
            playbackState.error == null &&
            !showQualityDialog &&
            !showInfoDialog
        ) {
            delay(3_000)
            showCtrl = false
        }
    }

    DisposableEffect(owner, playerView, player) {
        val lifecycle = owner.lifecycle
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            PlayerViewTargetBinder.bind(playerView, player)
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> PlayerViewTargetBinder.bind(playerView, player)
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

    Box(
        modifier = modifier
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { playerView },
            update = { view ->
                view.keepScreenOn = playbackState.playWhenReady
            },
            modifier = Modifier.fillMaxSize()
        )

        if (danmakuOverlayState != null) {
            DanmakuLayer(
                playerView = playerView,
                overlayState = danmakuOverlayState,
                danmakuState = liveDanmakuState,
                danmakuConfig = settingsState.danmaku,
                positionMs = 0L,
                isPlaying = playbackState.isPlaying,
                speed = 1f,
                seekEventId = 0L,
                hasSource = playbackState.playbackSource != null,
                renderMode = DanmakuRenderMode.LiveAppend
            )

            LiveDanmakuEffect(
                roomSessionState = roomSessionState,
                overlayState = danmakuOverlayState,
                playbackRoomId = roomId,
                danmakuOn = danmakuOn
            )
        }

        val cover = route?.cover
        if (!playbackState.hasRenderedFirstFrame && !cover.isNullOrBlank()) {
            BiliAsyncImage(
                url = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                variant = BiliImageVariant.Banner
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = tapSrc,
                    indication = null
                ) {
                    showCtrl = !showCtrl
                }
        )

        PlaybackBufferingOverlay(
            visible = playbackState.error == null &&
                playbackState.playerError.isNullOrBlank() &&
                (playbackState.isPreparing || playbackState.playbackState == PlaybackState.Buffering),
            modifier = Modifier.align(Alignment.Center)
        )

        if (showCtrl) {
            IconButton(
                onClick = {
                    showCtrl = true
                    onToggleDanmaku(!danmakuOn)
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 20.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (danmakuOn) Icons.Default.Subtitles else Icons.Default.SubtitlesOff,
                    contentDescription = if (danmakuOn) "关闭弹幕" else "开启弹幕",
                    tint = Color.White
                )
            }

            PlayerBottomControls(
                isPlaying = playbackState.isPlaying,
                playContentDescription = if (playbackState.isPlaying) "暂停" else "播放",
                timeText = "",
                progressValue = 0f,
                progressEnabled = false,
                showProgress = false,
                isFullscreen = isFull,
                fullscreenContentDescription = if (isFull) "退出全屏" else "全屏",
                onTogglePlay = {
                    showCtrl = true
                    onTogglePlay()
                },
                playEnabled = playbackState.playbackSource != null,
                onToggleFullscreen = {
                    showCtrl = true
                    onToggleFull()
                },
                onProgressChange = {},
                onProgressChangeFinished = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                optionActions = {
                    PlayerControlTextButton(
                        label = "画质",
                        text = qualityText,
                        enabled = (playbackState.playbackSource?.qualityOptions?.size ?: 0) > 1,
                        onClick = {
                            showCtrl = true
                            showQualityDialog = true
                            showInfoDialog = false
                        }
                    )
                    PlayerControlTextButton(
                        text = "信息",
                        onClick = {
                            showCtrl = true
                            showInfoDialog = true
                            showQualityDialog = false
                        }
                    )
                }
            )
        }

        if (showInfoDialog && useSidebar) {
            LivePlaybackInfoDialog(
                state = playbackState,
                onDismiss = { showInfoDialog = false },
                embedded = true
            )
        }

        if (showQualityDialog && useSidebar && qualityOptions != null) {
            PlaybackOptionSidebar(
                embedded = true,
                title = "选择画质",
                options = qualityOptions,
                onDismiss = { showQualityDialog = false },
                onSelect = { quality ->
                    onSwitchQuality(quality.toInt())
                    showQualityDialog = false
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxSize()
            )
        }

        if (playbackState.error != null && !playbackState.isPreparing) {
            Surface(
                color = Color.Black.copy(alpha = 0.42f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.small)
                    .clickable {
                        showCtrl = true
                        onRetry()
                    }
            ) {
                Text(
                    text = "重试",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }

    if (showQualityDialog && !useSidebar && qualityOptions != null) {
        PlaybackOptionBottomSheet(
            title = "选择画质",
            options = qualityOptions,
            onDismiss = { showQualityDialog = false },
            onSelect = { quality ->
                onSwitchQuality(quality.toInt())
                showQualityDialog = false
            }
        )
    }

    if (showInfoDialog && !useSidebar) {
        LivePlaybackInfoDialog(
            state = playbackState,
            onDismiss = { showInfoDialog = false }
        )
    }
}

@Composable
private fun LiveDanmakuEffect(
    roomSessionState: StateFlow<LiveRoomSessionState>,
    overlayState: DanmakuOverlayState,
    playbackRoomId: Long,
    danmakuOn: Boolean
) {
    var lastHandledMessageId by remember(playbackRoomId) {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(playbackRoomId, danmakuOn) {
        overlayState.clearLiveDanmakus()
        lastHandledMessageId = 0L
    }

    LaunchedEffect(roomSessionState, playbackRoomId, danmakuOn) {
        if (!danmakuOn || playbackRoomId <= 0L) {
            return@LaunchedEffect
        }
        roomSessionState
            .map { session ->
                Triple(
                    session.roomId,
                    session.messages.lastOrNull()?.localId ?: 0L,
                    session.messages
                )
            }
            .distinctUntilChanged()
            .collectLatest { (roomId, _, messages) ->
                if (roomId != playbackRoomId) {
                    lastHandledMessageId = 0L
                    return@collectLatest
                }
                if (messages.isEmpty()) {
                    return@collectLatest
                }
                val startIndex = messages.indexOfFirst { it.localId > lastHandledMessageId }
                if (startIndex < 0) {
                    return@collectLatest
                }
                val newItems = messages.subList(startIndex, messages.size)
                newItems.asSequence()
                    .filter(LiveRoomMessage::shouldRenderLiveDanmaku)
                    .forEach { msg ->
                        overlayState.appendDanmaku(msg.toLiveDanmakuItem())
                    }
                lastHandledMessageId = newItems.last().localId
            }
    }
}

@Composable
private fun LivePlaybackInfoDialog(
    state: LivePlaybackViewState,
    onDismiss: () -> Unit,
    embedded: Boolean = false
) {
    PlaybackSidePanel(title = "播放信息", onDismiss = onDismiss, embedded = embedded) {
            Text(
                text = buildLiveInfoText(state),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodyMedium
            )
    }
}

internal fun liveStatusText(status: LiveStatus): String {
    return when (status) {
        LiveStatus.Offline -> "未开播"
        LiveStatus.Living -> "直播中"
        LiveStatus.Round -> "轮播中"
    }
}

private fun liveResolutionText(state: LivePlaybackViewState): String {
    val width = state.videoWidth.takeIf { it > 0 } ?: return "未知"
    val height = state.videoHeight.takeIf { it > 0 } ?: return "未知"
    return "${width}x$height"
}

private fun buildLiveInfoText(
    state: LivePlaybackViewState
): String {
    val source = state.playbackSource
        ?: return if (state.isPreparing) "正在加载播放信息" else "暂无播放信息"

    val sections = buildList {
        state.error?.let { add("请求错误\n错误: ${it.toUiMessage()}") }
        state.playerError?.let { add("播放器错误\n错误: $it") }
        add(
            buildString {
                appendLine("直播")
                appendLine("状态: ${liveStatusText(source.liveStatus)}")
                append("画质: ${source.currentDescription}")
            }
        )
        add(
            buildString {
                appendLine("流信息")
                appendLine("协议: ${source.protocol}")
                appendLine("格式: ${source.format}")
                appendLine("编码: ${source.codec}")
                appendLine("分辨率: ${liveResolutionText(state)}")
                source.session?.takeIf(String::isNotBlank)?.let { appendLine("会话: $it") }
                appendLine("主地址: ${source.primaryUrl}")
                if (source.backupUrls.isNotEmpty()) {
                    append("备用地址:\n${source.backupUrls.joinToString("\n")}")
                }
            }
        )
        add(
            buildString {
                appendLine("播放器")
                appendLine("状态: ${livePlaybackStateText(state)}")
                appendLine("首帧: ${if (state.hasRenderedFirstFrame) "已渲染" else "未渲染"}")
                append("自动起播: ${if (state.playWhenReady) "开启" else "关闭"}")
            }
        )
    }

    return sections.joinToString("\n\n")
}

private fun livePlaybackStateText(state: LivePlaybackViewState): String {
    return when {
        state.isPlaying -> "播放中"
        state.isPreparing -> "准备中"
        else -> when (state.playbackState) {
            PlaybackState.Buffering -> "缓冲中"
            PlaybackState.Ready -> "已暂停"
            PlaybackState.Ended -> "已结束"
            PlaybackState.Idle -> "未开始"
        }
    }
}

private fun LiveRoomMessage.shouldRenderLiveDanmaku(): Boolean {
    return !isMirror && content.isNotBlank()
}

private fun LiveRoomMessage.toLiveDanmakuItem(): DanmakuItem {
    return DanmakuItem(
        id = localId,
        idStr = msgId ?: localId.toString(),
        progressMs = 0,
        mode = mode,
        fontSize = fontSize,
        color = color,
        midHash = user?.uid?.takeIf { it > 0L }?.toString().orEmpty(),
        content = content,
        createdAtEpochSecond = (sendTimeMs / 1000L).coerceAtLeast(0L),
        weight = 0,
        action = "",
        pool = 0,
        attr = 0,
        likeCount = 0L,
        animation = "",
        extra = extra.orEmpty(),
        colorfulType = 0,
        type = 0,
        oid = 0L,
        dmFromType = 0
    )
}
