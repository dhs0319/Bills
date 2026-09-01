package com.dhs0319.bills.feature.video

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.dhs0319.bills.core.settings.AppSettings
import com.dhs0319.bills.core.playback.VideoPlaybackController
import com.dhs0319.bills.core.model.CommentSubject
import com.dhs0319.bills.core.model.CommentSubjectTool
import com.dhs0319.bills.core.model.DanmakuConfig
import com.dhs0319.bills.core.model.PlayBiz
import com.dhs0319.bills.core.model.PlaybackProgress
import com.dhs0319.bills.core.model.PlayerBufferProfile
import com.dhs0319.bills.core.model.VideoCdnMode
import com.dhs0319.bills.core.model.VideoDownloadKind
import com.dhs0319.bills.core.model.VideoDownloadMeta
import com.dhs0319.bills.core.model.VideoDownloadRequest
import com.dhs0319.bills.core.model.VideoPlaybackState
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.isSameEntry
import com.dhs0319.bills.feature.video.action.VideoActionController
import com.dhs0319.bills.feature.video.action.VideoActionSeed
import com.dhs0319.bills.feature.video.action.VideoActionUiState
import com.dhs0319.bills.core.video.VideoActionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltViewModel
class VideoViewModel @Inject constructor(
    private val playbackController: VideoPlaybackController,
    private val playerSettings: AppSettings,
    private val videoActionRepository: VideoActionRepository
) : ViewModel() {

    private val _targetStack = MutableStateFlow<List<VideoTarget>>(emptyList())

    val player: StateFlow<Player?> = playbackController.player
    val videoState: StateFlow<VideoPlaybackState> = playbackController.videoState
    val playbackProgress: StateFlow<PlaybackProgress> = playbackController.playbackProgress
    val settingsState = playerSettings.state

    internal val videoActions = VideoActionController(videoActionRepository, viewModelScope)
    internal val actionUiState: StateFlow<VideoActionUiState> = videoActions.state

    init {
        viewModelScope.launch {
            videoState
                .map { state ->
                    VideoActionSeed(
                        aid = state.ids.aid,
                        detailLoaded = state.detail != null,
                        liked = state.detail?.isLiked == true,
                        favorited = state.detail?.isFavorited == true,
                        userCoinCount = state.detail?.userCoinCount ?: 0
                    )
                }
                .distinctUntilChanged()
                .collect { seed ->
                    videoActions.sync(seed)
                }
        }
    }

    val commentSubject: CommentSubject?
        get() {
            val src = currentTarget()?.src ?: return null
            val aid = videoState.value.ids.aid.takeIf { it > 0L } ?: return null
            return CommentSubjectTool.video(aid, src)
        }

    internal val danmakuState = playbackController.danmakuState

    fun openRoot(target: VideoTarget) {
        _targetStack.value = listOf(target)
        playbackController.openVideo(target)
    }

    fun openTarget(target: VideoTarget) {
        val current = currentTarget()
        if (current == target) return
        _targetStack.value = when {
            current == null -> listOf(target)
            current.isSameEntry(target) -> _targetStack.value.dropLast(1) + target
            else -> _targetStack.value + target
        }
        playbackController.openVideo(target)
    }

    fun popPage(): Boolean {
        val stack = _targetStack.value
        if (stack.size <= 1) return false
        val nextStack = stack.dropLast(1)
        val nextTarget = nextStack.last()
        _targetStack.value = nextStack
        playbackController.openVideo(nextTarget)
        return true
    }

    fun togglePlayPause() {
        if (videoState.value.isPlaying) {
            playbackController.pause()
        } else {
            playbackController.play()
        }
    }

    fun toggleLike() {
        videoActions.toggleLike()
    }

    fun submitCoins(amount: Int, onSuccess: () -> Unit) {
        videoActions.submitCoins(amount, onSuccess)
    }

    fun loadFavoriteFolders(onLoaded: (List<com.dhs0319.bills.core.video.VideoFavoriteFolder>) -> Unit) {
        videoActions.loadFavoriteFolders(onLoaded)
    }

    fun saveFavoriteFolders(
        originalFolderIds: Set<Long>,
        selectedFolderIds: Set<Long>,
        onSuccess: () -> Unit
    ) {
        videoActions.saveFavoriteFolders(originalFolderIds, selectedFolderIds, onSuccess)
    }

    fun consumeActionMessage() {
        videoActions.consumeMessage()
    }

    fun pause(): Boolean {
        val wasPlaying = videoState.value.isPlaying
        if (wasPlaying) {
            playbackController.pause()
        }
        return wasPlaying
    }

    fun resume() {
        playbackController.play()
    }

    fun switchQuality(quality: Int) {
        playbackController.switchVideoQuality(quality)
    }

    fun switchAudio(audioId: Int) {
        playbackController.switchVideoAudio(audioId)
    }

    fun updateVideoCdnMode(mode: VideoCdnMode) {
        viewModelScope.launch {
            playerSettings.setVideoCdnMode(mode)
        }
    }

    fun seekTo(positionMs: Long) {
        playbackController.seekTo(positionMs)
    }

    fun setSpeed(speed: Float) {
        playbackController.setSpeed(speed)
    }

    fun updateBackgroundPlayback(enabled: Boolean) {
        viewModelScope.launch {
            playerSettings.setBackgroundPlayback(enabled)
        }
    }

    fun updateInAppMiniPlayer(enabled: Boolean) {
        viewModelScope.launch {
            playerSettings.setInAppMiniPlayer(enabled)
        }
    }

    fun updateReportPlayback(enabled: Boolean) {
        viewModelScope.launch {
            playerSettings.setReportPlayback(enabled)
        }
    }

    fun updateBufferProfile(profile: PlayerBufferProfile) {
        viewModelScope.launch {
            playerSettings.setBufferProfile(profile)
        }
    }

    fun updatePreferSoftwareDecode(enabled: Boolean) {
        viewModelScope.launch {
            playerSettings.setPreferSoftwareDecode(enabled)
        }
    }

    fun updateDecoderFallback(enabled: Boolean) {
        viewModelScope.launch {
            playerSettings.setDecoderFallback(enabled)
        }
    }

    fun updateAutoRotateFullscreen(enabled: Boolean) {
        viewModelScope.launch {
            playerSettings.setAutoRotateFullscreen(enabled)
        }
    }

    fun updateGestureSpeed(speed: Float) {
        viewModelScope.launch {
            playerSettings.setGestureSpeed(speed)
        }
    }

    fun updateDanmaku(config: DanmakuConfig) {
        viewModelScope.launch {
            playerSettings.setDanmaku(config)
        }
    }

    fun switchPage(cid: Long) {
        val pageTarget = currentTarget() as? VideoTarget.Ugc ?: return
        val ids = videoState.value.ids
        if (ids.cid == cid) return
        if (ids.aid <= 0L || cid <= 0L) return
        val nextTarget = VideoTarget.Ugc(
            aid = ids.aid,
            cid = cid,
            bvid = ids.bvid,
            src = pageTarget.src
        )
        _targetStack.value = _targetStack.value.dropLast(1) + nextTarget
        playbackController.openVideo(nextTarget)
    }

    fun switchEpisode(target: VideoTarget) {
        val cur = currentTarget() ?: return
        if (cur == target) return
        _targetStack.value = _targetStack.value.dropLast(1) + target
        playbackController.openVideo(target)
    }

    fun currentDownloadRequest(
        kind: VideoDownloadKind,
        videoQuality: Int,
        audioQuality: Int
    ): VideoDownloadRequest? {
        val state = videoState.value
        state.detail ?: return null
        currentTarget() ?: return null
        val ids = state.ids
        if (!ids.hasAny) return null
        val meta = buildDownloadMeta()
        return VideoDownloadRequest(
            biz = state.biz,
            aid = ids.aid,
            cid = ids.cid,
            bvid = ids.bvid,
            epId = ids.epId,
            seasonId = ids.seasonId,
            kind = kind,
            videoQuality = videoQuality,
            audioQuality = audioQuality,
            meta = meta
        )
    }

    private fun buildDownloadMeta(): VideoDownloadMeta {
        val detail = videoState.value.detail
        val cid = videoState.value.ids.cid.takeIf { it > 0L }
        val part = detail?.pages?.firstOrNull { it.cid == cid }
        val title = detail?.let {
            listOfNotNull(
                it.title.takeIf(String::isNotBlank),
                part?.part?.takeIf(String::isNotBlank)
            ).joinToString(" - ").takeIf(String::isNotBlank)
        }
        return VideoDownloadMeta(
            title = title,
            cover = detail?.cover,
            ownerUid = detail?.owner?.mid?.takeIf { it > 0L },
            ownerName = detail?.owner?.name?.takeIf(String::isNotBlank)
        )
    }

    private fun currentTarget(): VideoTarget? {
        return _targetStack.value.lastOrNull()
    }
}
