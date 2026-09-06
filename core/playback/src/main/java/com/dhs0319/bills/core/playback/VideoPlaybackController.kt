package com.dhs0319.bills.core.playback

import androidx.media3.common.Player
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.PlaybackProgress
import com.dhs0319.bills.core.model.VideoPlaybackState
import com.dhs0319.bills.core.model.VideoTarget
import kotlinx.coroutines.flow.StateFlow

interface VideoPlaybackController {
    val player: StateFlow<Player?>
    val videoState: StateFlow<VideoPlaybackState>
    val playbackProgress: StateFlow<PlaybackProgress>
    val downloadSpeedBytesPerSecond: StateFlow<Long>
    val danmakuState: StateFlow<DanmakuSessionState>

    fun openVideo(target: VideoTarget)
    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun switchVideoQuality(quality: Int)
    fun switchVideoAudio(audioId: Int)
}
