package com.dhs0319.bills.feature.video.action

import androidx.compose.runtime.Immutable
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
    val favorited: Boolean = false,
    val userCoinCount: Int = 0,
    val likeCountDelta: Int = 0,
    val coinCountDelta: Int = 0,
    val favoriteCountDelta: Int = 0,
    val likeBusy: Boolean = false,
    val coinBusy: Boolean = false,
    val favoriteLoading: Boolean = false,
    val favoriteSaving: Boolean = false,
    val message: String? = null
)

internal data class VideoActionSeed(
    val aid: Long,
    val detailLoaded: Boolean,
    val liked: Boolean,
    val favorited: Boolean,
    val userCoinCount: Int
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

    fun sync(seed: VideoActionSeed) {
        val current = mutableState.value
        if (current.aid != seed.aid) {
            mutableState.value = VideoActionUiState(
                aid = seed.aid,
                initialized = seed.detailLoaded,
                liked = seed.liked,
                favorited = seed.favorited,
                userCoinCount = seed.userCoinCount
            )
        } else if (!current.initialized && seed.detailLoaded) {
            mutableState.update {
                it.copy(
                    initialized = true,
                    liked = seed.liked,
                    favorited = seed.favorited,
                    userCoinCount = seed.userCoinCount
                )
            }
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
