package com.naaammme.bbspace.infra.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import com.naaammme.bbspace.core.common.log.Logger
import com.naaammme.bbspace.core.model.PlaybackProgress
import com.naaammme.bbspace.core.model.PlaybackState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Suppress("UnsafeOptInUsageError")
@UnstableApi
@Singleton
class Media3PlayerEngine @Inject constructor(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient
) : PlayerEngine {

    private val appContext = context.applicationContext
    private val videoOkHttpClient = okHttpClient
    private val singleFileDashMediaSourceFactory = SingleFileDashMediaSourceFactory(
        appContext = appContext,
        okHttpClient = okHttpClient
    )

    private val _player = MutableStateFlow<Player?>(null)
    override val player: StateFlow<Player?> = _player.asStateFlow()
    private val _currentSource = MutableStateFlow<EngineSource?>(null)
    override val currentSource: StateFlow<EngineSource?> = _currentSource.asStateFlow()
    private val _playbackState = MutableStateFlow(PlayerPlaybackState())
    override val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()
    private val _playbackProgress = MutableStateFlow(PlaybackProgress())
    override val playbackProgress: StateFlow<PlaybackProgress> = _playbackProgress.asStateFlow()
    private val runtimeScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var playerConfig = PlayerConfig()
    private var videoDecoderName: String? = null
    private var audioDecoderName: String? = null
    private var firstFrameSeq = 0L
    private var lastEventsPlaybackState = Player.STATE_IDLE
    private var lastEventsIsPlaying = false
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason.isSeekDiscontinuity()) {
                updatePlaybackState { it.copy(seekEventSeq = it.seekEventSeq + 1L) }
            }
            updatePlaybackProgress()
        }

        override fun onPlayerError(error: PlaybackException) {
            Logger.e(TAG, error) {
                "player error code=${error.errorCodeName} msg=${error.message} " +
                        "videoDec=$videoDecoderName audioDec=$audioDecoderName"
            }
            // TODO: 出现 decoder reclaimed 等 system-level 错误时重建 player session，当前只记日志不恢复
            updatePlaybackState { it.copy(errorMessage = error.message) }
        }

        override fun onRenderedFirstFrame() {
            firstFrameSeq += 1L
            updatePlaybackState()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            val state = player.playbackState
            val playing = player.isPlaying
            if (state != lastEventsPlaybackState || playing != lastEventsIsPlaying) {
                lastEventsPlaybackState = state
                lastEventsIsPlaying = playing
                updatePlaybackState()
                updateProgressPolling()
            }
        }
    }
    private val analyticsListener = object : AnalyticsListener {
        override fun onVideoDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            videoDecoderName = decoderName
            updatePlaybackState()
        }

        override fun onAudioDecoderInitialized(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String,
            initializedTimestampMs: Long,
            initializationDurationMs: Long
        ) {
            audioDecoderName = decoderName
            updatePlaybackState()
        }

        override fun onVideoDecoderReleased(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String
        ) {
            if (videoDecoderName == decoderName) {
                videoDecoderName = null
                updatePlaybackState()
            }
        }

        override fun onAudioDecoderReleased(
            eventTime: AnalyticsListener.EventTime,
            decoderName: String
        ) {
            if (audioDecoderName == decoderName) {
                audioDecoderName = null
                updatePlaybackState()
            }
        }
    }
    private var exoPlayer: ExoPlayer? = null

    override fun updateConfig(config: PlayerConfig) {
        val next = normalizeConfig(config)
        if (next == playerConfig && exoPlayer != null) return

        val prev = exoPlayer
        stopProgressPolling()
        playerConfig = next
        resetRuntimeState()
        _currentSource.value = null
        val nextPlayer = buildPlayer(appContext, next)
        exoPlayer = nextPlayer
        _player.value = nextPlayer
        resetPlaybackFlows()
        prev?.release()
    }

    override suspend fun setSource(
        source: EngineSource,
        startPositionMs: Long?,
        playWhenReady: Boolean,
        metadata: MediaMetadata?
    ) {
        _currentSource.value = source
        try {
            val player = ensurePlayer()
            val itemMetadata = metadata ?: player.currentMediaItem?.mediaMetadata ?: MediaMetadata.EMPTY
            firstFrameSeq = 0L
            val mediaSource = buildMediaSource(source, itemMetadata)
            player.setMediaSource(mediaSource)
            if (startPositionMs != null && startPositionMs > 0) {
                player.seekTo(startPositionMs.coerceAtLeast(0L))
            }
            player.playWhenReady = playWhenReady
            player.prepare()
            updatePlaybackState { it.copy(errorMessage = null) }
            updatePlaybackProgress()
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            Logger.e(TAG, t) { "set source failed type=${source::class.simpleName} msg=${t.message}" }
            updatePlaybackState { it.copy(errorMessage = t.message ?: "加载媒体源失败") }
            updatePlaybackProgress()
            throw t
        }
    }

    override fun play() {
        val player = ensurePlayer()
        player.play()
    }

    override fun pause() {
        val player = exoPlayer ?: return
        player.pause()
    }

    override fun setSpeed(speed: Float) {
        val player = ensurePlayer()
        player.playbackParameters = PlaybackParameters(speed.coerceIn(0.25f, 3f))
        updatePlaybackState()
    }

    override fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        player.seekTo(positionMs.coerceAtLeast(0L))
        updatePlaybackProgress()
    }

    override fun setMediaMetadata(metadata: MediaMetadata) {
        val player = exoPlayer ?: return
        val current = player.currentMediaItem ?: return
        val next = current.buildUpon()
            .setMediaMetadata(metadata)
            .build()
        player.replaceMediaItem(player.currentMediaItemIndex, next)
    }

    override fun stopForReuse(resetPosition: Boolean) {
        stopProgressPolling()
        val player = exoPlayer ?: run {
            resetRuntimeState()
            _currentSource.value = null
            resetPlaybackFlows()
            return
        }
        resetRuntimeState()
        _currentSource.value = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        if (resetPosition) {
            player.seekTo(0)
        }
        resetPlaybackFlows()
    }

    override fun release() {
        stopProgressPolling()
        val player = exoPlayer ?: return
        resetRuntimeState()
        exoPlayer = null
        _player.value = null
        _currentSource.value = null
        resetPlaybackFlows()
        player.release()
    }

    private fun buildPlayer(context: Context, config: PlayerConfig): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            // .forceDisableMediaCodecAsynchronousQueueing()
            .setEnableDecoderFallback(config.decoderFallback)
            .setMediaCodecSelector(buildCodecSelector(config))

        return ExoPlayer.Builder(context, renderersFactory)
            .setLoadControl(buildLoadControl(config))
            .setWakeMode(C.WAKE_MODE_NONE)
            .build()
            .apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                setHandleAudioBecomingNoisy(true)
                addListener(playerListener)
                addAnalyticsListener(analyticsListener)
            }
    }

    private fun buildCodecSelector(config: PlayerConfig): MediaCodecSelector {
        return MediaCodecSelector { mimeType, requiresSecureDecoder, requiresTunnelingDecoder ->
            when (config.decoderMode) {
                DecoderMode.Hard -> {
                    val infos = MediaCodecSelector.DEFAULT
                        .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
                    infos.sortedBy { it.softwareOnly }
                }

                DecoderMode.Soft -> MediaCodecSelector.PREFER_SOFTWARE
                    .getDecoderInfos(mimeType, requiresSecureDecoder, requiresTunnelingDecoder)
            }
        }
    }

    private fun ensurePlayer(): ExoPlayer {
        exoPlayer?.let { return it }
        val player = buildPlayer(appContext, playerConfig)
        exoPlayer = player
        _player.value = player
        resetPlaybackFlows()
        return player
    }

    private fun buildLoadControl(config: PlayerConfig): DefaultLoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                config.minBufferMs,
                config.maxBufferMs,
                config.playBufferMs,
                config.rebufferMs
            )
            .setBackBuffer(config.backBufferMs, true)
            .build()
    }

    private fun normalizeConfig(config: PlayerConfig): PlayerConfig {
        val playMs = max(config.playBufferMs, 0)
        val rebufMs = max(config.rebufferMs, 0)
        val minBufMs = max(config.minBufferMs, max(playMs, rebufMs))
        val maxBufMs = max(config.maxBufferMs, minBufMs)
        return config.copy(
            minBufferMs = minBufMs,
            maxBufferMs = maxBufMs,
            playBufferMs = playMs,
            rebufferMs = rebufMs,
            backBufferMs = max(config.backBufferMs, 0)
        )
    }

    private suspend fun buildMediaSource(
        source: EngineSource,
        metadata: MediaMetadata
    ): MediaSource {
        return when (source) {
            is EngineSource.LiveFlv -> {
                val mediaSourceFactory = buildProgressiveMediaSourceFactory(source)
                val item = mediaItem(source.url, metadata)
                    .buildUpon()
                    .setLiveConfiguration(MediaItem.LiveConfiguration.Builder().build())
                    .build()
                mediaSourceFactory.createMediaSource(item)
            }

            is EngineSource.LocalMerged -> {
                val mediaSourceFactory = buildProgressiveMediaSourceFactory(source)
                val video = mediaSourceFactory.createMediaSource(mediaItem(source.videoUrl, metadata))
                if (source.audioUrl.isNullOrBlank()) {
                    video
                } else {
                    val audio = mediaSourceFactory.createMediaSource(mediaItem(source.audioUrl, metadata))
                    MergingMediaSource(true, video, audio)
                }
            }

            is EngineSource.SingleFileDash -> singleFileDashMediaSourceFactory.create(source, metadata)

            is EngineSource.Progressive -> {
                val mediaSourceFactory = buildProgressiveMediaSourceFactory(source)
                if (source.segments.size == 1) {
                    mediaSourceFactory.createMediaSource(
                        mediaItem(source.segments.first().url, metadata)
                    )
                } else {
                    val builder = ConcatenatingMediaSource2.Builder()
                    source.segments.forEach { segment ->
                        builder.add(
                            mediaSourceFactory.createMediaSource(mediaItem(segment.url, metadata)),
                            segment.durationMs ?: C.TIME_UNSET
                        )
                    }
                    builder.build()
                }
            }
        }
    }

    private fun buildProgressiveMediaSourceFactory(source: EngineSource): ProgressiveMediaSource.Factory {
        val requestSpec = source.toPlaybackRequestSpec()
        val upstreamFactory = OkHttpDataSource.Factory(videoOkHttpClient)
            .setUserAgent(requestSpec.userAgent)
        if (requestSpec.headers.isNotEmpty()) {
            upstreamFactory.setDefaultRequestProperties(requestSpec.headers)
        }
        return ProgressiveMediaSource.Factory(
            DefaultDataSource.Factory(appContext, upstreamFactory)
        )
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(1))
    }

    private fun mediaItem(uri: String, metadata: MediaMetadata): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun updatePlaybackState(
        transform: (PlayerPlaybackState) -> PlayerPlaybackState = { it }
    ) {
        val player = exoPlayer
        _playbackState.update { state ->
            transform(
                state.copy(
                    isPlaying = player?.isPlaying ?: false,
                    playWhenReady = player?.playWhenReady ?: false,
                    playbackState = (player?.playbackState ?: Player.STATE_IDLE).toPlaybackState(),
                    speed = player?.playbackParameters?.speed ?: 1f,
                    videoWidth = player?.videoSize?.width ?: 0,
                    videoHeight = player?.videoSize?.height ?: 0,
                    firstFrameSeq = firstFrameSeq,
                    videoDecoderName = videoDecoderName,
                    audioDecoderName = audioDecoderName
                )
            )
        }
    }

    private fun updatePlaybackProgress() {
        val player = exoPlayer
        _playbackProgress.value = PlaybackProgress(
            positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: 0L,
            bufferedPositionMs = player?.bufferedPosition?.coerceAtLeast(0L) ?: 0L,
            durationMs = player?.duration?.takeIf { it > 0L } ?: 0L
        )
    }

    private fun updateProgressPolling() {
        val player = exoPlayer
        val shouldPoll = player != null &&
            lastEventsIsPlaying &&
            lastEventsPlaybackState == Player.STATE_READY
        if (!shouldPoll) {
            stopProgressPolling()
            return
        }
        if (progressJob?.isActive == true) return
        progressJob?.cancel()
        progressJob = runtimeScope.launch {
            while (isActive) {
                delay(1_000)
                updatePlaybackProgress()
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun resetPlaybackFlows() {
        _playbackState.value = PlayerPlaybackState()
        _playbackProgress.value = PlaybackProgress()
    }

    private fun resetRuntimeState() {
        firstFrameSeq = 0L
        videoDecoderName = null
        audioDecoderName = null
        lastEventsPlaybackState = Player.STATE_IDLE
        lastEventsIsPlaying = false
    }

    private fun Int.toPlaybackState(): PlaybackState {
        return when (this) {
            Player.STATE_BUFFERING -> PlaybackState.Buffering
            Player.STATE_READY -> PlaybackState.Ready
            Player.STATE_ENDED -> PlaybackState.Ended
            else -> PlaybackState.Idle
        }
    }

    private fun Int.isSeekDiscontinuity(): Boolean {
        return this == Player.DISCONTINUITY_REASON_SEEK ||
            this == Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
    }

    private companion object {
        const val TAG = "Media3Player"
    }
}
