package com.dhs0319.bills.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dhs0319.bills.core.auth.AuthRepository
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.favorite.FavoriteRepository
import com.dhs0319.bills.core.model.FavoriteContentCursor
import com.dhs0319.bills.core.model.FavoriteContentItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repo: FavoriteRepository,
    private val authRepo: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        FavoriteUiState(isLoggedIn = authRepo.currentMidFlow.value > 0L)
    )
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    fun observeIsLoggedIn() = uiState
        .map { it.isLoggedIn }
        .distinctUntilChanged()

    fun observeSelectedTab() = uiState
        .map { it.tab }
        .distinctUntilChanged()

    fun observeContentState() = uiState
        .map { it.content }
        .distinctUntilChanged()

    fun observeFolderState() = uiState
        .map { it.folder }
        .distinctUntilChanged()

    private var contentJob: Job? = null
    private var folderJob: Job? = null
    private val deletingContent = mutableSetOf<Pair<Int, Long>>()
    private val removedContent = mutableSetOf<Pair<Int, Long>>()
    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()
    private val folderPreviewJobs = mutableMapOf<Long, Job>()
    private val folderPreviewSemaphore = Semaphore(3)
    private var authVersion = 0L

    init {
        viewModelScope.launch {
            authRepo.currentMidFlow.collect { mid ->
                authVersion += 1L
                contentJob?.cancel()
                folderJob?.cancel()
                folderPreviewJobs.values.toList().forEach { it.cancel() }
                folderPreviewJobs.clear()
                deletingContent.clear()
                removedContent.clear()
                _actionMessage.value = null
                val tab = _uiState.value.tab
                _uiState.value = FavoriteUiState(tab = tab, isLoggedIn = mid > 0L)
                if (mid > 0L) {
                    when (tab) {
                        FavoriteTab.CONTENT -> loadContent(reset = true, refreshing = false)
                        FavoriteTab.FOLDER -> loadFolders(refreshing = false)
                    }
                }
            }
        }
    }

    fun selectTab(tab: FavoriteTab) {
        val state = _uiState.value
        if (state.tab == tab) return
        val shouldLoad = when (tab) {
            FavoriteTab.CONTENT -> !state.content.hasLoaded
            FavoriteTab.FOLDER -> !state.folder.hasLoaded
        }
        _uiState.update { it.copy(tab = tab) }
        if (!state.isLoggedIn || !shouldLoad) return
        when (tab) {
            FavoriteTab.CONTENT -> loadContent(reset = true, refreshing = false)
            FavoriteTab.FOLDER -> loadFolders(refreshing = false)
        }
    }

    fun refresh() {
        if (!_uiState.value.isLoggedIn) return
        when (_uiState.value.tab) {
            FavoriteTab.CONTENT -> {
                removedContent.clear()
                contentJob?.cancel()
                loadContent(reset = true, refreshing = true)
            }

            FavoriteTab.FOLDER -> {
                folderJob?.cancel()
                loadFolders(refreshing = true)
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.isLoggedIn) return
        val state = _uiState.value.content
        if (!state.canLoadMore) return
        loadContent(reset = false, refreshing = false)
    }

    fun removeContent(item: FavoriteContentItem) {
        if (!_uiState.value.isLoggedIn) return
        val identity = item.otype to item.oid
        if (item.oid <= 0L || !deletingContent.add(identity)) return
        val version = authVersion
        viewModelScope.launch {
            try {
                repo.removeFromFavorites(item)
                if (version != authVersion) return@launch
                removedContent.add(identity)
                _uiState.update { state ->
                    state.copy(
                        content = state.content.copy(
                            items = withoutRemovedContent(state.content.items)
                        ),
                        folder = state.folder.copy(
                            previews = state.folder.previews.mapValues { (_, items) ->
                                withoutRemovedContent(items)
                            }
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version != authVersion) return@launch
                Logger.e(TAG, e) { "取消收藏失败" }
                _actionMessage.value = e.message?.takeIf(String::isNotBlank) ?: "取消收藏失败"
            } finally {
                if (version == authVersion) deletingContent.remove(identity)
            }
        }
    }

    fun consumeActionMessage() {
        _actionMessage.value = null
    }

    private fun withoutRemovedContent(items: List<FavoriteContentItem>): List<FavoriteContentItem> {
        return items.filterNot { (it.otype to it.oid) in removedContent }
    }

    fun loadFolderPreview(fid: Long) {
        if (!_uiState.value.isLoggedIn) return
        val state = _uiState.value.folder
        val folder = state.folders.firstOrNull { it.fid == fid } ?: return
        if (folder.mediaCount <= 0 || state.isRefreshing ||
            state.previews.containsKey(fid) || folderPreviewJobs[fid]?.isActive == true
        ) return

        folderPreviewJobs[fid] = viewModelScope.launch {
            try {
                val items = try {
                    folderPreviewSemaphore.withPermit {
                        repo.fetchFavoriteFolderContents(fid = fid, page = 1)
                            .items.take(FAVORITE_FOLDER_PREVIEW_COUNT)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(TAG, e) { "加载收藏夹预览失败 fid=$fid" }
                    emptyList()
                }
                ensureActive()
                _uiState.update {
                    it.copy(
                        folder = it.folder.copy(
                            previews = it.folder.previews + (
                                fid to withoutRemovedContent(items)
                            )
                        )
                    )
                }
            } finally {
                if (folderPreviewJobs[fid] === coroutineContext[Job]) {
                    folderPreviewJobs.remove(fid)
                }
            }
        }
    }

    private fun loadFolders(refreshing: Boolean) {
        if (!_uiState.value.isLoggedIn || folderJob?.isActive == true) return
        folderJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    folder = it.folder.copy(
                        isLoading = !refreshing && it.folder.folders.isEmpty(),
                        isRefreshing = refreshing,
                        errorMessage = null
                    )
                )
            }
            runCatching {
                repo.fetchMyFavorites()
            }.onSuccess { page ->
                ensureActive()
                folderPreviewJobs.values.toList().forEach { it.cancel() }
                folderPreviewJobs.clear()
                _uiState.update {
                    it.copy(
                        folder = it.folder.copy(
                            folders = page.folders,
                            previews = emptyMap(),
                            previewGeneration = it.folder.previewGeneration + 1,
                            hasLoaded = true,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    )
                }
            }.onFailure { e ->
                if (!isActive) return@launch
                Logger.e(TAG, e) { "加载收藏夹失败" }
                _uiState.update {
                    it.copy(
                        folder = it.folder.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = e.message ?: "加载收藏失败"
                        )
                    )
                }
            }
        }
    }

    private fun loadContent(
        reset: Boolean,
        refreshing: Boolean
    ) {
        if (!_uiState.value.isLoggedIn || contentJob?.isActive == true) return
        val reqCursor = if (reset) FavoriteContentCursor() else _uiState.value.content.cursor
        contentJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    content = it.content.copy(
                        isLoading = !refreshing && reset && it.content.items.isEmpty(),
                        isRefreshing = refreshing,
                        isLoadingMore = !reset,
                        errorMessage = null,
                        errorOnLoadMore = false
                    )
                )
            }
            runCatching {
                repo.fetchFavoriteContents(reqCursor)
            }.onSuccess { page ->
                ensureActive()
                _uiState.update {
                    it.copy(
                        content = it.content.copy(
                            items = withoutRemovedContent(
                                if (reset) page.items else it.content.items + page.items
                            ),
                            hasLoaded = true,
                            cursor = page.cursor,
                            hasMore = page.hasMore,
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            errorMessage = null,
                            errorOnLoadMore = false
                        )
                    )
                }
            }.onFailure { e ->
                if (!isActive) return@launch
                Logger.e(TAG, e) { "加载全部收藏失败" }
                _uiState.update {
                    it.copy(
                        content = it.content.copy(
                            isLoading = false,
                            isRefreshing = false,
                            isLoadingMore = false,
                            errorMessage = e.message ?: "加载收藏失败",
                            errorOnLoadMore = !reset
                        )
                    )
                }
            }
        }
    }

    private companion object {
        const val TAG = "FavoriteViewModel"
    }
}
