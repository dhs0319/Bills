package com.dhs0319.bills.core.playback

import androidx.media3.common.Player
import com.dhs0319.bills.core.model.DownloadPlaybackState
import kotlinx.coroutines.flow.StateFlow

interface DownloadPlaybackController {
    val player: StateFlow<Player?>
    val state: StateFlow<DownloadPlaybackState>

    suspend fun open(taskId: Long)

    fun play()
    fun pause()
    fun seekTo(positionMs: Long)
    fun setSpeed(speed: Float)
    fun release()
}
