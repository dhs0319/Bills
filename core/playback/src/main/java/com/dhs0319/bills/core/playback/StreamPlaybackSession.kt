package com.dhs0319.bills.core.playback

import androidx.media3.common.Player
import com.dhs0319.bills.core.model.StreamPlaybackSessionState
import com.dhs0319.bills.core.model.StreamPlaybackTarget
import kotlinx.coroutines.flow.StateFlow

interface StreamPlaybackSession {
    val player: StateFlow<Player?>
    val currentTarget: StateFlow<StreamPlaybackTarget?>
    val sessionState: StateFlow<StreamPlaybackSessionState>

    suspend fun prepare()

    fun play()
    fun pause()
    fun close()
}
