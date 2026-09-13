package com.dhs0319.bills.feature.video.player

import android.app.Activity
import android.media.AudioManager
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.icon.AppIcons
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

@Stable
class VideoGestureState {
    var dragType by mutableStateOf(DragType.None)
        internal set
    var dragFraction by mutableFloatStateOf(0f)
        internal set
    var dragSeekPosMs by mutableStateOf<Long?>(null)
        internal set
    var dragSeekDeltaMs by mutableLongStateOf(0L)
        internal set

    var seekLabel by mutableStateOf<String?>(null)
        internal set
    var showSpeedBadge by mutableStateOf(false)
        internal set
    var speedBadgeText by mutableStateOf("2x")
        internal set
    var doubleTapHint by mutableStateOf<DoubleTapHint?>(null)
        internal set

    internal var doubleTapToken by mutableLongStateOf(0L)
    internal var dragStartPosMs by mutableLongStateOf(0L)

    internal fun resetDrag() {
        dragType = DragType.None
        dragFraction = 0f
        dragSeekPosMs = null
        dragSeekDeltaMs = 0L
        seekLabel = null
    }

    internal fun showDoubleTap(hint: DoubleTapHint) {
        doubleTapHint = hint
        doubleTapToken++
    }
}

enum class DragType { None, Seek, Brightness, Volume }

enum class DoubleTapHint(val text: String) {
    Play("播放"),
    Pause("暂停"),
    Rewind("-10s"),
    Forward("+10s")
}

private const val SEEK_MAX_MS = 60_000L
private const val DOUBLE_TAP_SEEK_MS = 10_000L
private const val DRAG_SENSITIVITY = 0.6f
private const val SIDE_GESTURE_ZONE = 0.2f
private const val RIGHT_GESTURE_ZONE_START = 1f - SIDE_GESTURE_ZONE
private val FullscreenVerticalDragBlockExtra = 8.dp
private val TopGestureFeedbackPadding = 72.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.videoGestures(
    state: VideoGestureState,
    onToggleControls: () -> Unit,
    onTogglePlay: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onStartSpeedUp: () -> String,
    onStopSpeedUp: () -> Unit,
    onBrightnessDelta: (Float) -> Float,
    onVolumeDelta: (Float) -> Float,
    onDragStart: (DragType) -> Unit = {},
    onDragEnd: () -> Unit = {},
    isPlaying: () -> Boolean,
    positionMs: () -> Long,
    durationMs: () -> Long
): Modifier {
    val curToggleControls by rememberUpdatedState(onToggleControls)
    val curTogglePlay by rememberUpdatedState(onTogglePlay)
    val curSeekTo by rememberUpdatedState(onSeekTo)
    val curStartSpeedUp by rememberUpdatedState(onStartSpeedUp)
    val curStopSpeedUp by rememberUpdatedState(onStopSpeedUp)
    val curBrightnessDelta by rememberUpdatedState(onBrightnessDelta)
    val curVolumeDelta by rememberUpdatedState(onVolumeDelta)
    val curDragStart by rememberUpdatedState(onDragStart)
    val curDragEnd by rememberUpdatedState(onDragEnd)
    val curIsPlaying by rememberUpdatedState(isPlaying)
    val curPositionMs by rememberUpdatedState(positionMs)
    val curDurationMs by rememberUpdatedState(durationMs)
    val visibleStatusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val hiddenStatusBarHeight = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues()
        .calculateTopPadding()
    val topVerticalDragBlockHeight = if (visibleStatusBarHeight == 0.dp) {
        hiddenStatusBarHeight + FullscreenVerticalDragBlockExtra
    } else {
        0.dp
    }

    return this.pointerInput(topVerticalDragBlockHeight) {
        val slop = viewConfiguration.touchSlop
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val doubleTapTimeout = viewConfiguration.doubleTapTimeoutMillis
        val topVerticalDragBlockPx = topVerticalDragBlockHeight.toPx()

        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown()
                val downTime = down.uptimeMillis
                val downPos = down.position
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) continue

                val zoneX = downPos.x / w

                var phase = Phase.Press
                var totalDelta = Offset.Zero
                var lastPos = downPos

                while (true) {
                    val event = if (phase == Phase.Press) {
                        val remaining = (longPressTimeout - (SystemClock.uptimeMillis() - downTime))
                            .coerceAtLeast(0L)
                        withTimeoutOrNull(remaining) {
                            awaitPointerEvent(PointerEventPass.Main)
                        }
                    } else {
                        awaitPointerEvent(PointerEventPass.Main)
                    }
                    if (event == null) {
                        phase = Phase.LongPress
                        state.speedBadgeText = curStartSpeedUp()
                        state.showSpeedBadge = true
                        continue
                    }
                    val change = event.changes.firstOrNull() ?: break

                    if (!change.pressed) {
                        when (phase) {
                            Phase.Press -> {
                                val second = withTimeoutOrNull(doubleTapTimeout) {
                                    awaitFirstDown()
                                }
                                if (second != null) {
                                    val sndX = second.position.x / w
                                    when {
                                        sndX < SIDE_GESTURE_ZONE -> {
                                            val target = (curPositionMs() - DOUBLE_TAP_SEEK_MS).coerceAtLeast(0L)
                                            curSeekTo(target)
                                            state.showDoubleTap(DoubleTapHint.Rewind)
                                        }
                                        sndX > RIGHT_GESTURE_ZONE_START -> {
                                            val target = (curPositionMs() + DOUBLE_TAP_SEEK_MS).coerceAtMost(curDurationMs())
                                            curSeekTo(target)
                                            state.showDoubleTap(DoubleTapHint.Forward)
                                        }
                                        else -> {
                                            curTogglePlay()
                                            state.showDoubleTap(
                                                if (curIsPlaying()) DoubleTapHint.Play
                                                else DoubleTapHint.Pause
                                            )
                                        }
                                    }
                                    waitForAllUp()
                                } else {
                                    curToggleControls()
                                }
                            }
                            Phase.Drag -> {
                                if (state.dragType == DragType.Seek) {
                                    curSeekTo(state.dragSeekPosMs ?: state.dragStartPosMs)
                                }
                                state.resetDrag()
                                curDragEnd()
                            }
                            Phase.LongPress -> {
                                curStopSpeedUp()
                                state.showSpeedBadge = false
                            }
                        }
                        break
                    }

                    val delta = change.position - lastPos
                    totalDelta += delta
                    lastPos = change.position

                    when (phase) {
                        Phase.Press -> {
                            if (totalDelta.getDistance() >= slop) {
                                val isHorizontal = abs(totalDelta.x) > abs(totalDelta.y)
                                phase = Phase.Drag
                                val dragType = when {
                                    isHorizontal -> DragType.Seek
                                    downPos.y <= topVerticalDragBlockPx -> DragType.None
                                    zoneX < SIDE_GESTURE_ZONE -> DragType.Brightness
                                    zoneX > RIGHT_GESTURE_ZONE_START -> DragType.Volume
                                    else -> DragType.None
                                }
                                state.dragStartPosMs = curPositionMs()
                                state.dragType = dragType
                                curDragStart(dragType)
                                when (dragType) {
                                    DragType.Brightness -> state.dragFraction = curBrightnessDelta(0f)
                                    DragType.Volume -> state.dragFraction = curVolumeDelta(0f)
                                    else -> Unit
                                }
                            }
                        }
                        Phase.Drag -> {
                            when (state.dragType) {
                                DragType.Seek -> {
                                    val frac = totalDelta.x / (w * DRAG_SENSITIVITY)
                                    val deltaMs = (frac * SEEK_MAX_MS).roundToLong()
                                    val newPos = (state.dragStartPosMs + deltaMs)
                                        .coerceIn(0L, curDurationMs())
                                    val actualDeltaMs = newPos - state.dragStartPosMs
                                    state.dragFraction = frac.coerceIn(-1f, 1f)
                                    state.dragSeekPosMs = newPos
                                    state.dragSeekDeltaMs = actualDeltaMs
                                    state.seekLabel = formatSeekDelta(actualDeltaMs)
                                }
                                DragType.Brightness -> {
                                    val deltaFrac = (-totalDelta.y / (h * DRAG_SENSITIVITY)).coerceIn(-1f, 1f)
                                    state.dragFraction = curBrightnessDelta(deltaFrac)
                                }
                                DragType.Volume -> {
                                    val deltaFrac = (-totalDelta.y / (h * DRAG_SENSITIVITY)).coerceIn(-1f, 1f)
                                    state.dragFraction = curVolumeDelta(deltaFrac)
                                }
                                DragType.None -> {}
                            }
                        }
                        Phase.LongPress -> {
                        }
                    }

                    change.consume()
                }
            }
        }
    }
}

@Composable
fun VideoGestureFeedback(
    state: VideoGestureState,
    durationMs: Long,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.dragType == DragType.Brightness || state.dragType == DragType.Volume) {
            val isBrightness = state.dragType == DragType.Brightness
            val barH = state.dragFraction.coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isBrightness) Icons.Default.Brightness6 else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (isBrightness) "亮度" else "音量",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(100.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(Color.White.copy(alpha = 0.24f)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height((100 * barH).dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(Color.White)
                        )
                    }
                }
            }
        }

        if (state.dragType == DragType.Seek && state.seekLabel != null) {
            val positionMs = (state.dragSeekPosMs ?: state.dragStartPosMs)
                .coerceIn(0L, durationMs.coerceAtLeast(0L))
            val progress = if (durationMs > 0L) {
                positionMs.toFloat() / durationMs.toFloat()
            } else {
                0f
            }
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TopGestureFeedbackPadding)
                    .widthIn(max = 200.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${formatGestureTime(positionMs)} / ${formatGestureTime(durationMs)} [${formatGestureDelta(state.dragSeekDeltaMs)}]",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                ) {
                    val activeWidth = size.width * progress.coerceIn(0f, 1f)
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.3f),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    )
                    drawRoundRect(
                        color = Color.White,
                        size = androidx.compose.ui.geometry.Size(activeWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f)
                    )
                }
            }
        }

        val hint = state.doubleTapHint
        if (hint != null) {
            val token = state.doubleTapToken
            var visible by remember(token) { mutableStateOf(true) }
            LaunchedEffect(token) {
                visible = true
                delay(400)
                visible = false
                state.doubleTapHint = null
            }
            if (visible) {
                if (hint == DoubleTapHint.Play || hint == DoubleTapHint.Pause) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = if (hint == DoubleTapHint.Pause) AppIcons.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (hint == DoubleTapHint.Pause) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (hint == DoubleTapHint.Rewind) {
                                Icons.Default.FastRewind
                            } else {
                                Icons.Default.FastForward
                            },
                            contentDescription = hint.text,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = hint.text,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        if (state.showSpeedBadge) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = TopGestureFeedbackPadding)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FastForward,
                    contentDescription = "倍速播放",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = state.speedBadgeText,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun formatGestureTime(valueMs: Long): String {
    val totalSeconds = (valueMs / 1_000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun formatGestureDelta(valueMs: Long): String {
    val sign = if (valueMs >= 0L) "+" else "-"
    val absoluteSeconds = kotlin.math.abs(valueMs / 1_000L)
    return "$sign%02d:%02d".format(absoluteSeconds / 60L, absoluteSeconds % 60L)
}

private enum class Phase { Press, Drag, LongPress }

private suspend fun AwaitPointerEventScope.waitForAllUp() {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.all { !it.pressed }) return
    }
}

private fun formatSeekDelta(ms: Long): String {
    val totalSec = ms / 1000
    if (totalSec == 0L) return "0s"
    val sign = if (totalSec > 0) "+" else ""
    return "$sign${totalSec}s"
}

internal fun adjustWindowBrightness(activity: Activity?, fraction: Float) {
    val win = activity?.window ?: return
    win.attributes = win.attributes.apply {
        screenBrightness = fraction.coerceIn(0.01f, 1f)
    }
}

internal fun adjustStreamVolume(audioManager: AudioManager?, volume: Int) {
    val am = audioManager ?: return
    val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    am.setStreamVolume(AudioManager.STREAM_MUSIC, volume.coerceIn(0, maxVolume), 0)
}
