package com.dhs0319.bills.feature.home.paging

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomePagingState<T>(
    val items: List<T> = emptyList(),
    val refreshBoundaryIndex: Int? = null,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isInitialLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val loadMoreError: String? = null
)

internal data class HomePage<T, K>(
    val items: List<T>,
    val nextKey: K?,
    val onAccepted: () -> Unit = {}
)

/** Main-thread owner of a tab's requests. Backend cursors stay in each tab's loader. */
internal class HomePager<T, K : Any>(
    private val scope: CoroutineScope,
    private val initialKey: () -> K,
    private val itemKey: (T) -> Any,
    private val loadPage: suspend (K) -> HomePage<T, K>,
    private val onStateChanged: (HomePagingState<T>) -> Unit = {},
    private val onError: (Exception) -> Unit = {},
    private val batchSize: Int = 20,
    private val maxPagesPerLoad: Int = 3,
    private val prependOnRefresh: Boolean = false
) {
    private val _state = MutableStateFlow(HomePagingState<T>())
    val state = _state.asStateFlow()
    private var nextKey: K? = null
    private var acceptedItemKeys = mutableSetOf<Any>()
    private var request: Job? = null
    private var generation = 0
    private var handledRefreshRequest = 0

    fun activate(refreshRequest: Int): Boolean {
        if (refreshRequest > handledRefreshRequest) {
            handledRefreshRequest = refreshRequest
            refresh()
            return true
        }
        if (!state.value.hasLoaded && state.value.errorMessage == null) refresh()
        return false
    }

    fun refresh(restart: Boolean = false) {
        if (state.value.isRefreshing && !restart) return
        start(refresh = true, key = initialKey())
    }

    fun loadMore() {
        val current = state.value
        if (!current.hasLoaded || !current.hasMore || current.isRefreshing ||
            current.isLoadingMore || current.errorMessage != null || current.loadMoreError != null
        ) return
        start(refresh = false, key = nextKey ?: return)
    }

    fun retryLoadMore() {
        if (state.value.isRefreshing || state.value.isLoadingMore) return
        publish(state.value.copy(loadMoreError = null))
        loadMore()
    }

    private fun publish(value: HomePagingState<T>) {
        _state.value = value
        onStateChanged(value)
    }

    private fun start(refresh: Boolean, key: K) {
        val version = ++generation
        val oldItems = if (refresh && prependOnRefresh) state.value.items else emptyList()
        val keepOldCursor = oldItems.isNotEmpty()
        val oldNextKey = if (keepOldCursor) nextKey else null
        request?.cancel()
        // Set the guard before launching, including with a queued dispatcher.
        publish(state.value.copy(
            isRefreshing = refresh,
            isLoadingMore = !refresh,
            isInitialLoading = refresh && (state.value.isInitialLoading || state.value.items.isEmpty()),
            errorMessage = null,
            loadMoreError = null
        ))
        request = scope.launch {
            var acceptedPage = false
            var refreshedItems = emptyList<T>()
            try {
                var cursor = key
                var addedCount = 0
                // Reuse the keys accepted by previous pages so appends never scan
                // the entire feed on the UI thread as the list grows.
                val seenKeys = if (refresh && !prependOnRefresh) mutableSetOf() else acceptedItemKeys
                val visitedCursors = mutableSetOf<K>()
                for (pageIndex in 0 until maxPagesPerLoad) {
                    visitedCursors.add(cursor)
                    val page = loadPage(cursor)
                    currentCoroutineContext().ensureActive()
                    if (version != generation) return@launch
                    val pageKeys = mutableSetOf<Any>()
                    val additions = page.items.filter { item ->
                        val key = itemKey(item)
                        key !in seenKeys && pageKeys.add(key)
                    }
                    addedCount += additions.size
                    nextKey = page.nextKey?.takeUnless { it in visitedCursors }
                    val items = when {
                        refresh && prependOnRefresh -> {
                            refreshedItems = refreshedItems + additions
                            refreshedItems + oldItems
                        }
                        refresh && !acceptedPage -> additions
                        else -> state.value.items + additions
                    }
                    acceptedPage = true
                    page.onAccepted()
                    seenKeys.addAll(pageKeys)
                    acceptedItemKeys = seenKeys
                    // Publish each successful page immediately, while filling the buffer.
                    publish(state.value.copy(items = items, hasLoaded = true,
                        refreshBoundaryIndex = if (refresh && prependOnRefresh && refreshedItems.isNotEmpty()) {
                            refreshedItems.size.takeIf { it < items.size }
                        } else {
                            state.value.refreshBoundaryIndex
                        },
                        hasMore = if (keepOldCursor) oldNextKey != null else nextKey != null))
                    if (addedCount >= batchSize) break
                    cursor = nextKey ?: break
                }
                if (addedCount == 0 && nextKey != null && !keepOldCursor) {
                    publish(state.value.copy(loadMoreError = "暂未获取到新内容，请点击重试"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (version != generation) return@launch
                onError(e)
                val message = e.message?.takeIf { it.isNotBlank() } ?: "加载失败，请重试"
                publish(if (refresh && (!acceptedPage || keepOldCursor)) state.value.copy(errorMessage = message)
                    else state.value.copy(loadMoreError = message))
            } finally {
                if (version == generation) {
                    // The visible tail is still the old list, so continue paging from its cursor.
                    if (keepOldCursor) nextKey = oldNextKey
                    publish(state.value.copy(isRefreshing = false, isLoadingMore = false, isInitialLoading = false))
                }
            }
        }
    }
}
