package com.dhs0319.bills.feature.video

import androidx.compose.runtime.Immutable
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
import com.dhs0319.bills.core.video.VideoActionRepository
import com.dhs0319.bills.core.video.VideoFavoriteFolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Immutable
internal data class VideoActionUiState(
    val aid: Long = 0L,
    val initialized: Boolean = false,
    val liked: Boolean = false,
    val favorited: Boolean = false,
    val userCoinCount: Int = 0,
    val likeCountDelta: Int = 0,
    val coinCountDelta: Int = 0,
    val favoriteCountDelta: Int = 0,
    val likeBusy: Boolean = false,
    val coinBusy: Boolean = false,
    val coinSheetVisible: Boolean = false,
    val selectedCoinAmount: Int = 1,
    val favoriteLoading: Boolean = false,
    val favoriteSaving: Boolean = false,
    val favoriteDialogVisible: Boolean = false,
    val favoriteFolders: List<VideoFavoriteFolder> = emptyList(),
    val originalFolderIds: Set<Long> = emptySet(),
    val selectedFolderIds: Set<Long> = emptySet(),
    val message: String? = null
)

private data class VideoActionSeed(
    val aid: Long,
    val detailLoaded: Boolean,
    val liked: Boolean,
    val favorited: Boolean,
    val userCoinCount: Int
)

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

    private val _actionUiState = MutableStateFlow(VideoActionUiState())
    internal val actionUiState: StateFlow<VideoActionUiState> = _actionUiState

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
                    val current = _actionUiState.value
                    if (current.aid != seed.aid) {
                        _actionUiState.value = VideoActionUiState(
                            aid = seed.aid,
                            initialized = seed.detailLoaded,
                            liked = seed.liked,
                            favorited = seed.favorited,
                            userCoinCount = seed.userCoinCount
                        )
                    } else if (!current.initialized && seed.detailLoaded) {
                        _actionUiState.update {
                            it.copy(
                                initialized = true,
                                liked = seed.liked,
                                favorited = seed.favorited,
                                userCoinCount = seed.userCoinCount
                            )
                        }
                    }
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
        val current = _actionUiState.value
        val aid = current.aid
        if (aid <= 0L || current.likeBusy) return
        val nextLiked = !current.liked
        val delta = if (nextLiked) 1 else -1
        _actionUiState.update {
            it.copy(
                liked = nextLiked,
                likeCountDelta = it.likeCountDelta + delta,
                likeBusy = true,
                message = null
            )
        }
        viewModelScope.launch {
            runCatching { videoActionRepository.setLiked(aid, nextLiked) }
                .onSuccess { toast ->
                    updateActionState(aid) {
                        it.copy(
                            likeBusy = false,
                            message = toast ?: if (nextLiked) {
                                "点赞成功"
                            } else {
                                "已取消点赞"
                            }
                        )
                    }
                }
                .onFailure { error ->
                    updateActionState(aid) {
                        it.copy(
                            liked = current.liked,
                            likeCountDelta = current.likeCountDelta,
                            likeBusy = false,
                            message = error.actionMessage("点赞失败")
                        )
                    }
                }
        }
    }

    fun openCoinPicker() {
        _actionUiState.update { state ->
            if (!state.initialized || state.aid <= 0L || state.coinBusy) return@update state
            state.copy(
                coinSheetVisible = true,
                selectedCoinAmount = 1,
                message = null
            )
        }
    }

    fun selectCoinAmount(amount: Int) {
        _actionUiState.update { state ->
            if (!state.coinSheetVisible || state.coinBusy || amount !in 1..2) {
                state
            } else {
                state.copy(selectedCoinAmount = amount)
            }
        }
    }

    fun dismissCoinPicker() {
        _actionUiState.update {
            if (it.coinBusy) it else it.copy(coinSheetVisible = false)
        }
    }

    fun submitCoins() {
        val current = _actionUiState.value
        val aid = current.aid
        val amount = current.selectedCoinAmount
        if (
            aid <= 0L ||
            !current.coinSheetVisible ||
            current.coinBusy ||
            amount !in 1..2
        ) return

        _actionUiState.update { it.copy(coinBusy = true, message = null) }
        viewModelScope.launch {
            runCatching { videoActionRepository.addCoins(aid, amount) }
                .onSuccess {
                    updateActionState(aid) {
                        it.copy(
                            userCoinCount = current.userCoinCount + amount,
                            coinCountDelta = current.coinCountDelta + amount,
                            coinBusy = false,
                            coinSheetVisible = false,
                            message = "成功投出${amount}枚硬币"
                        )
                    }
                }
                .onFailure { error ->
                    updateActionState(aid) {
                        it.copy(
                            coinBusy = false,
                            message = error.actionMessage("投币失败")
                        )
                    }
                }
        }
    }

    fun openFavoritePicker() {
        val current = _actionUiState.value
        val aid = current.aid
        if (aid <= 0L || current.favoriteLoading || current.favoriteSaving) return
        _actionUiState.update { it.copy(favoriteLoading = true, message = null) }
        viewModelScope.launch {
            runCatching { videoActionRepository.fetchFavoriteFolders(aid) }
                .onSuccess { folders ->
                    updateActionState(aid) { state ->
                        if (folders.isEmpty()) {
                            state.copy(
                                favoriteLoading = false,
                                message = "暂无可用收藏夹"
                            )
                        } else {
                            val selectedIds = folders.filter(VideoFavoriteFolder::selected)
                                .mapTo(linkedSetOf(), VideoFavoriteFolder::id)
                            state.copy(
                                favoriteLoading = false,
                                favoriteDialogVisible = true,
                                favoriteFolders = folders,
                                originalFolderIds = selectedIds,
                                selectedFolderIds = selectedIds
                            )
                        }
                    }
                }
                .onFailure { error ->
                    updateActionState(aid) {
                        it.copy(
                            favoriteLoading = false,
                            message = error.actionMessage("加载收藏夹失败")
                        )
                    }
                }
        }
    }

    fun selectFavoriteFolder(folderId: Long, selected: Boolean) {
        _actionUiState.update { state ->
            if (!state.favoriteDialogVisible || state.favoriteSaving) return@update state
            state.copy(
                selectedFolderIds = if (selected) {
                    state.selectedFolderIds + folderId
                } else {
                    state.selectedFolderIds - folderId
                }
            )
        }
    }

    fun dismissFavoritePicker() {
        _actionUiState.update {
            if (it.favoriteSaving) it else it.copy(favoriteDialogVisible = false)
        }
    }

    fun saveFavoriteFolders() {
        val current = _actionUiState.value
        val aid = current.aid
        if (aid <= 0L || !current.favoriteDialogVisible || current.favoriteSaving) return
        val addIds = current.selectedFolderIds - current.originalFolderIds
        val removeIds = current.originalFolderIds - current.selectedFolderIds
        if (addIds.isEmpty() && removeIds.isEmpty()) {
            dismissFavoritePicker()
            return
        }
        _actionUiState.update { it.copy(favoriteSaving = true, message = null) }
        viewModelScope.launch {
            runCatching {
                videoActionRepository.updateFavorites(aid, addIds, removeIds)
            }.onSuccess {
                updateActionState(aid) { state ->
                    val nextFavorited = current.selectedFolderIds.isNotEmpty()
                    val countDelta = when {
                        nextFavorited == current.favorited -> current.favoriteCountDelta
                        nextFavorited -> current.favoriteCountDelta + 1
                        else -> current.favoriteCountDelta - 1
                    }
                    state.copy(
                        favorited = nextFavorited,
                        favoriteCountDelta = countDelta,
                        favoriteSaving = false,
                        favoriteDialogVisible = false,
                        originalFolderIds = current.selectedFolderIds,
                        favoriteFolders = current.favoriteFolders.map { folder ->
                            folder.copy(selected = folder.id in current.selectedFolderIds)
                        },
                        message = if (nextFavorited) "收藏成功" else "已取消收藏"
                    )
                }
            }.onFailure { error ->
                updateActionState(aid) {
                    it.copy(
                        favoriteSaving = false,
                        message = error.actionMessage("收藏操作失败")
                    )
                }
            }
        }
    }

    fun consumeActionMessage() {
        _actionUiState.update { it.copy(message = null) }
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

    private inline fun updateActionState(
        aid: Long,
        transform: (VideoActionUiState) -> VideoActionUiState
    ) {
        _actionUiState.update { state ->
            if (state.aid == aid) transform(state) else state
        }
    }
}

private fun Throwable.actionMessage(fallback: String): String {
    return message?.takeIf(String::isNotBlank) ?: fallback
}
