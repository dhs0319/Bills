package com.dhs0319.bills.feature.favorite.folderdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.favorite.FavoriteRepository
import com.dhs0319.bills.core.model.FavoriteContentItem
import com.dhs0319.bills.feature.favorite.navigation.FAVORITE_FOLDER_COUNT_ARG
import com.dhs0319.bills.feature.favorite.navigation.FAVORITE_FOLDER_CREATED_AT_ARG
import com.dhs0319.bills.feature.favorite.navigation.FAVORITE_FOLDER_TITLE_ARG
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class FavoriteFolderDetailViewModel @Inject constructor(
    private val repo: FavoriteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val fid = savedStateHandle.get<Long>(FID_ARG) ?: 0L

    private val _uiState = MutableStateFlow(
        FavoriteFolderDetailUiState(
            title = savedStateHandle.get<String>(FAVORITE_FOLDER_TITLE_ARG)
                ?.takeIf { it.isNotBlank() } ?: "收藏夹",
            totalCount = savedStateHandle.get<Int>(FAVORITE_FOLDER_COUNT_ARG) ?: 0,
            createdAtSec = savedStateHandle.get<Long>(FAVORITE_FOLDER_CREATED_AT_ARG) ?: 0L,
            isLoading = true
        )
    )
    val uiState: StateFlow<FavoriteFolderDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private val deletingItems = mutableSetOf<Pair<Int, Long>>()
    private val removedItems = mutableSetOf<Pair<Int, Long>>()
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        if (fid > 0L) {
            loadPage(reset = true, refreshing = false)
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "收藏夹不存在"
                )
            }
        }
    }

    fun refresh() {
        removedItems.clear()
        loadJob?.cancel()
        loadPage(reset = true, refreshing = true)
    }

    fun loadMore() {
        if (!_uiState.value.canLoadMore) return
        loadPage(reset = false, refreshing = false)
    }

    fun removeItem(item: FavoriteContentItem) {
        val identity = item.otype to item.oid
        if (item.oid <= 0L || !deletingItems.add(identity)) return
        viewModelScope.launch {
            try {
                repo.removeFromFavoriteFolder(item, fid)
                removedItems.add(identity)
                val removedCount = _uiState.value.items.count {
                    it.otype == item.otype && it.oid == item.oid
                }
                _uiState.update { state ->
                    state.copy(
                        items = withoutRemovedItems(state.items),
                        totalCount = (state.totalCount - removedCount).coerceAtLeast(0)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.e(TAG, e) { "取消收藏失败 fid=$fid" }
                _actionMessage.value = e.message?.takeIf(String::isNotBlank) ?: "取消收藏失败"
            } finally {
                deletingItems.remove(identity)
            }
        }
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    private fun withoutRemovedItems(items: List<FavoriteContentItem>): List<FavoriteContentItem> {
        return items.filterNot { (it.otype to it.oid) in removedItems }
    }

    private fun loadPage(
        reset: Boolean,
        refreshing: Boolean
    ) {
        if (fid <= 0L || loadJob?.isActive == true) return
        val reqPage = if (reset) 1 else _uiState.value.page + 1
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !refreshing && reset && it.items.isEmpty(),
                    isRefreshing = refreshing,
                    isLoadingMore = !reset,
                    errorMessage = null,
                    errorOnLoadMore = false
                )
            }
            runCatching {
                repo.fetchFavoriteFolderContents(fid = fid, page = reqPage)
            }.onSuccess { page ->
                _uiState.update {
                    val items = withoutRemovedItems(
                        if (reset) page.items else it.items + page.items
                    )
                    it.copy(
                        items = items,
                        page = reqPage,
                        totalCount = if (removedItems.isEmpty()) {
                            page.totalCount
                        } else {
                            minOf(page.totalCount, it.totalCount)
                        },
                        hasInvalid = page.hasInvalid,
                        hasMore = page.hasMore,
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = null,
                        errorOnLoadMore = false
                    )
                }
            }.onFailure { e ->
                if (!isActive) return@launch
                Logger.e(TAG, e) { "加载收藏夹内容失败 fid=$fid page=$reqPage" }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        errorMessage = e.message ?: "加载收藏夹失败",
                        errorOnLoadMore = !reset
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "FavoriteFolderDetailVm"
        const val FID_ARG = "fid"
    }
}
