package com.dhs0319.bills.feature.video.player

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import com.dhs0319.bills.core.playback.subtitle.VideoSubtitleState
import kotlin.math.ceil

@Composable
internal fun VideoSubtitleLayer(
    player: Player?,
    state: VideoSubtitleState,
    isFull: Boolean,
    showControls: Boolean
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val primary = state.primary
    val secondary = state.secondary
    var caption by remember(player, primary, secondary) { mutableStateOf("") }

    DisposableEffect(player, primary, secondary, lifecycle) {
        if (player == null || primary == null || primary.isEmpty && secondary?.isEmpty != false) {
            caption = ""
            onDispose {}
        } else {
            val handler = Handler(Looper.getMainLooper())
            var started = lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            var disposed = false
            val refresh = object : Runnable {
                override fun run() {
                    handler.removeCallbacks(this)
                    if (!started || disposed || player.playbackState == Player.STATE_IDLE ||
                        player.playbackState == Player.STATE_ENDED) {
                        caption = ""
                        return
                    }
                    val position = player.currentPosition.coerceAtLeast(0L)
                    val first = primary.cueAt(position)?.text.orEmpty()
                    val second = secondary?.cueAt(position)?.text.orEmpty()
                    caption = when {
                        first.isEmpty() -> second
                        second.isEmpty() || second == first -> first
                        else -> "$first\n$second"
                    }
                    if (!player.isPlaying) return
                    val primaryChange = primary.nextChangeAfter(position)
                    val secondaryChange = secondary?.nextChangeAfter(position)
                    val next = when {
                        primaryChange == null -> secondaryChange
                        secondaryChange == null -> primaryChange
                        else -> minOf(primaryChange, secondaryChange)
                    } ?: return
                    val speed = player.playbackParameters.speed.toDouble().coerceAtLeast(0.01)
                    val delayMs = ceil((next - position) / speed).toLong().coerceAtLeast(8L)
                    // Exactly one pending wake-up at the next caption boundary, independent of FPS.
                    handler.postDelayed(this, delayMs)
                }
            }
            val listener = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    if (events.containsAny(
                            Player.EVENT_POSITION_DISCONTINUITY,
                            Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
                            Player.EVENT_PLAYBACK_STATE_CHANGED,
                            Player.EVENT_IS_PLAYING_CHANGED,
                            Player.EVENT_MEDIA_ITEM_TRANSITION
                        )) {
                        refresh.run()
                    }
                }
            }
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_START -> {
                        started = true
                        refresh.run()
                    }
                    Lifecycle.Event.ON_STOP -> {
                        started = false
                        handler.removeCallbacks(refresh)
                        caption = ""
                    }
                    else -> Unit
                }
            }
            player.addListener(listener)
            lifecycle.addObserver(observer)
            refresh.run()
            onDispose {
                disposed = true
                handler.removeCallbacks(refresh)
                player.removeListener(listener)
                lifecycle.removeObserver(observer)
            }
        }
    }

    if (caption.isNotEmpty()) {
        val fontSize = when {
            state.largeText && isFull -> 24.sp
            state.largeText -> 18.sp
            isFull -> 18.sp
            else -> 14.sp
        }
        Box(Modifier.fillMaxSize()
            .padding(horizontal = if (isFull) 28.dp else 16.dp)
            .padding(bottom = if (showControls) 96.dp else if (isFull) 20.dp else 12.dp)) {
            Text(
                text = caption,
                color = Color.White,
                fontSize = fontSize,
                lineHeight = fontSize * 1.35f,
                textAlign = TextAlign.Center,
                maxLines = if (state.bilingual) 4 else 3,
                modifier = Modifier.align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(5.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
