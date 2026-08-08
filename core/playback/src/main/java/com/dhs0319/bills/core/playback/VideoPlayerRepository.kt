package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.model.PlaybackRequest
import com.dhs0319.bills.core.model.PlaybackSource

interface VideoPlayerRepository {
    suspend fun fetchPlaybackSource(request: PlaybackRequest): PlaybackSource
}

