package com.dhs0319.bills.core.playback.di

import com.dhs0319.bills.core.playback.DownloadPlaybackController
import com.dhs0319.bills.core.playback.DownloadPlaybackControllerImpl
import com.dhs0319.bills.core.playback.LivePlaybackController
import com.dhs0319.bills.core.playback.StreamPlaybackSession
import com.dhs0319.bills.core.playback.StreamPlaybackSessionImpl
import com.dhs0319.bills.core.playback.VideoPlaybackController
import com.dhs0319.bills.core.playback.VideoPlayerRepository
import com.dhs0319.bills.core.playback.repository.VideoPlayerRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaybackModule {

    @Binds
    @Singleton
    abstract fun bindVideoPlayerRepository(impl: VideoPlayerRepositoryImpl): VideoPlayerRepository

    @Binds
    @Singleton
    abstract fun bindStreamPlaybackSession(impl: StreamPlaybackSessionImpl): StreamPlaybackSession

    @Binds
    @Singleton
    abstract fun bindVideoPlaybackController(impl: StreamPlaybackSessionImpl): VideoPlaybackController

    @Binds
    @Singleton
    abstract fun bindLivePlaybackController(impl: StreamPlaybackSessionImpl): LivePlaybackController

    @Binds
    @Singleton
    abstract fun bindDownloadPlaybackController(
        impl: DownloadPlaybackControllerImpl
    ): DownloadPlaybackController
}
