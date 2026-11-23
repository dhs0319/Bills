package com.dhs0319.bills.feature.video.action

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.VideoDetail
import com.dhs0319.bills.core.video.VideoActionRepository
import com.dhs0319.bills.core.video.VideoFavoriteFolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Shared video-action state. Screen-specific sheet state belongs to each screen. */
@Immutable
internal data class VideoActionUiState(
    val aid: Long = 0L,
    val initialized: Boolean = false,
    val liked: Boolean = false,
    val disliked: Boolean = false,
    val favorited: Boolean = false,
    val userCoinCount: Int = 0,
    val followingByMid: Map<Long, Boolean> = emptyMap(),
    val likeCountDelta: Int = 0,
    val coinCountDelta: Int = 0,
    val favoriteCountDelta: Int = 0,
    val likeBusy: Boolean = false,
    val dislikeBusy: Boolean = false,
    val coinBusy: Boolean = false,
    val favoriteLoading: Boolean = false,
    val favoriteSaving: Boolean = false,
    val followBusyMids: Set<Long> = emptySet(),
    val dislikeThanksVisible: Boolean = false,
    val message: String? = null
)

/**
 * Owns the behavior shared by every video surface, while leaving action-sheet
 * visibility, selection and presentation entirely to the calling screen.
 */
internal class VideoActionController(
    private val repository: VideoActionRepository,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow(VideoActionUiState())
    val state: StateFlow<VideoActionUiState> = mutableState

    fun sync(aid: Long, detail: VideoDetail?, detailLoading: Boolean) {
        val detailLoaded = detail != null && !detailLoading
        val current = mutableState.value
        if (current.aid == aid && (current.initialized || !detailLoaded)) return

        val followingByMid = buildMap {
            detail?.staffs?.forEach { member ->
                if (member.mid > 0L) put(member.mid, member.isFollowing)
            }
            detail?.owner?.let { owner ->
                if (owner.mid > 0L) put(owner.mid, owner.isFollowing)
            }
        }
        mutableState.update { state ->
            val base = if (state.aid == aid) state else {
                VideoActionUiState(aid = aid, followBusyMids = state.followBusyMids)
            }
            base.copy(
                initialized = detailLoaded,
                liked = detail?.isLiked == true,
                disliked = detail?.isDisliked == true,
                favorited = detail?.isFavorited == true,
                userCoinCount = detail?.userCoinCount ?: 0,
                followingByMid = followingByMid
            )
        }
    }

    fun toggleLike() {
        val current = mutableState.value
        val aid = current.aid
        if (aid <= 0L || current.likeBusy) return
        val nextLiked = !current.liked
        val delta = if (nextLiked) 1 else -1
        mutableState.update {
            it.copy(
                liked = nextLiked,
                likeCountDelta = it.likeCountDelta + delta,
                likeBusy = true,
                message = null
            )
        }
        scope.launch {
            runCatching { repository.setLiked(aid, nextLiked) }
                .onSuccess { toast ->
                    updateForAid(aid) {
                        it.copy(
                            likeBusy = false,
                            message = toast ?: if (nextLiked) "点赞成功" else "已取消点赞"
                        )
                    }
                }
                .onFailure { error ->
                    updateForAid(aid) {
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

    fun toggleDislike() {
        val current = mutableState.value
        val aid = current.aid
        if (aid <= 0L || current.dislikeBusy) return
        val nextDisliked = !current.disliked
        mutableState.update { it.copy(dislikeBusy = true, message = null) }
        scope.launch {
            runCatching { repository.setDisliked(aid, nextDisliked) }
                .onSuccess { toast ->
                    updateForAid(aid) {
                        it.copy(
                            disliked = nextDisliked,
                            dislikeBusy = false,
                            dislikeThanksVisible = nextDisliked,
                            message = if (nextDisliked) null else toast ?: "已取消不喜欢"
                        )
                    }
                }
                .onFailure { error ->
                    updateForAid(aid) {
                        it.copy(
                            dislikeBusy = false,
                            message = error.actionMessage("操作失败")
                        )
                    }
                }
        }
    }

    fun toggleFollow(mid: Long) {
        val current = mutableState.value
        val following = current.followingByMid[mid] ?: return
        if (!current.initialized || mid in current.followBusyMids) return
        val nextFollowing = !following
        mutableState.update { it.copy(followBusyMids = it.followBusyMids + mid, message = null) }
        scope.launch {
            val result = runCatching { repository.setFollowing(mid, nextFollowing) }
            val message = result.fold(
                onSuccess = { if (nextFollowing) "关注成功" else "已取消关注" },
                onFailure = { it.actionMessage("操作失败") }
            )
            mutableState.update { state ->
                val followingByMid = if (result.isSuccess && mid in state.followingByMid) {
                    state.followingByMid + (mid to nextFollowing)
                } else {
                    state.followingByMid
                }
                state.copy(
                    followingByMid = followingByMid,
                    followBusyMids = state.followBusyMids - mid,
                    message = if (state.aid == current.aid) message else state.message
                )
            }
        }
    }

    fun submitCoins(amount: Int, onSuccess: () -> Unit) {
        val current = mutableState.value
        val aid = current.aid
        if (aid <= 0L || current.coinBusy || amount !in 1..2) return

        mutableState.update { it.copy(coinBusy = true, message = null) }
        scope.launch {
            runCatching { repository.addCoins(aid, amount) }
                .onSuccess {
                    updateForAid(aid) {
                        it.copy(
                            userCoinCount = current.userCoinCount + amount,
                            coinCountDelta = current.coinCountDelta + amount,
                            coinBusy = false,
                            message = "成功投出${amount}枚硬币"
                        )
                    }
                    if (mutableState.value.aid == aid) onSuccess()
                }
                .onFailure { error ->
                    updateForAid(aid) {
                        it.copy(coinBusy = false, message = error.actionMessage("投币失败"))
                    }
                }
        }
    }

    fun loadFavoriteFolders(onLoaded: (List<VideoFavoriteFolder>) -> Unit) {
        val current = mutableState.value
        val aid = current.aid
        if (aid <= 0L || current.favoriteLoading || current.favoriteSaving) return
        mutableState.update { it.copy(favoriteLoading = true, message = null) }
        scope.launch {
            runCatching { repository.fetchFavoriteFolders(aid) }
                .onSuccess { folders ->
                    updateForAid(aid) {
                        it.copy(
                            favoriteLoading = false,
                            message = if (folders.isEmpty()) "暂无可用收藏夹" else null
                        )
                    }
                    if (folders.isNotEmpty() && mutableState.value.aid == aid) {
                        onLoaded(folders)
                    }
                }
                .onFailure { error ->
                    updateForAid(aid) {
                        it.copy(
                            favoriteLoading = false,
                            message = error.actionMessage("加载收藏夹失败")
                        )
                    }
                }
        }
    }

    fun saveFavoriteFolders(
        originalFolderIds: Set<Long>,
        selectedFolderIds: Set<Long>,
        onSuccess: () -> Unit
    ) {
        val current = mutableState.value
        val aid = current.aid
        if (aid <= 0L || current.favoriteSaving) return
        val addIds = selectedFolderIds - originalFolderIds
        val removeIds = originalFolderIds - selectedFolderIds
        if (addIds.isEmpty() && removeIds.isEmpty()) {
            onSuccess()
            return
        }

        mutableState.update { it.copy(favoriteSaving = true, message = null) }
        scope.launch {
            runCatching { repository.updateFavorites(aid, addIds, removeIds) }
                .onSuccess {
                    updateForAid(aid) { state ->
                        val nextFavorited = selectedFolderIds.isNotEmpty()
                        val countDelta = when {
                            nextFavorited == current.favorited -> current.favoriteCountDelta
                            nextFavorited -> current.favoriteCountDelta + 1
                            else -> current.favoriteCountDelta - 1
                        }
                        state.copy(
                            favorited = nextFavorited,
                            favoriteCountDelta = countDelta,
                            favoriteSaving = false,
                            message = if (nextFavorited) "收藏成功" else "已取消收藏"
                        )
                    }
                    if (mutableState.value.aid == aid) onSuccess()
                }
                .onFailure { error ->
                    updateForAid(aid) {
                        it.copy(
                            favoriteSaving = false,
                            message = error.actionMessage("收藏操作失败")
                        )
                    }
                }
        }
    }

    fun consumeMessage() {
        mutableState.update { it.copy(message = null) }
    }

    fun consumeDislikeThanks() {
        mutableState.update { it.copy(dislikeThanksVisible = false) }
    }

    private inline fun updateForAid(
        aid: Long,
        transform: (VideoActionUiState) -> VideoActionUiState
    ) {
        mutableState.update { state ->
            if (state.aid == aid) transform(state) else state
        }
    }
}

private fun Throwable.actionMessage(fallback: String): String {
    return message?.takeIf(String::isNotBlank) ?: fallback
}
