package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.danmaku.VodDanmakuRepository
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.DanmakuWindow
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.core.model.VodDanmakuRequest
import com.dhs0319.bills.core.model.danmakuWindowStartMs
import com.dhs0319.bills.core.model.toDanmakuWindowId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class VodDanmakuSession(
    private val scope: CoroutineScope,
    private val repository: VodDanmakuRepository
) {
    private val _state = MutableStateFlow(DanmakuSessionState())
    val state: StateFlow<DanmakuSessionState> = _state.asStateFlow()

    private var loadingJob: Job? = null
    private var loadingWindowId: Long? = null
    private var prefetchJob: Job? = null
    private var prefetchWindowId: Long? = null
    private var currentIds: ResolvedVideoIds? = null
    private var currentDurationMs = 0L
    private var currentWindowId: Long? = null

    fun setSource(
        ids: ResolvedVideoIds?,
        durationMs: Long
    ) {
        if (ids == null || !ids.danmakuReady) {
            reset()
            return
        }

        if (currentIds != ids) {
            resetSource(ids, durationMs)
        } else {
            currentIds = ids
            currentDurationMs = durationMs.coerceAtLeast(0L)
            updateSourceKey(ids.toDanmakuSourceKey())
        }
    }

    fun seekTo(positionMs: Long) {
        currentWindowId = null
        ensureWindowAt(positionMs)
    }

    fun onProgress(positionMs: Long) {
        ensureWindowAt(positionMs)
    }

    private fun ensureWindowAt(positionMs: Long) {
        val ids = currentIds ?: return
        val windowId = positionMs.coerceAtLeast(0L).toDanmakuWindowId()
        if (windowId == currentWindowId) return
        currentWindowId = windowId
        updateSourceKey(ids.toDanmakuSourceKey())
        usePrefetchedWindow(windowId)
        ensureWindowLoaded(windowId)
        prefetchNextWindow()
    }

    /** 命中预取分段时直接切换，避免在分段边界等待网络。 */
    private fun usePrefetchedWindow(windowId: Long) {
        val prefetched = _state.value.prefetchWindow ?: return
        if (prefetched.id != windowId) return
        _state.update { it.copy(window = prefetched, prefetchWindow = null) }
    }

    /** 预取下一分段，播放到边界时数据已就绪。 */
    private fun prefetchNextWindow() {
        val current = currentWindowId ?: return
        val windowId = current + 1L
        val state = _state.value
        if (state.window?.id == windowId || state.prefetchWindow?.id == windowId) return
        if (prefetchWindowId == windowId && prefetchJob?.isActive == true) return
        if (state.prefetchWindow != null) {
            _state.update { it.copy(prefetchWindow = null) }
        }
        if (!isWindowWithinDuration(windowId)) return
        val request = buildRequest(windowId) ?: return
        prefetchJob?.cancel()
        prefetchWindowId = windowId
        prefetchJob = scope.launch {
            runCatching {
                repository.fetchSegment(request)
            }.onSuccess { segment ->
                if (segment.request.ids != currentIds || currentWindowId != windowId - 1L) {
                    return@onSuccess
                }
                _state.update { currentState ->
                    if (currentState.window?.id == windowId) {
                        currentState
                    } else {
                        currentState.copy(
                            prefetchWindow = DanmakuWindow(windowId, segment.items)
                        )
                    }
                }
            }
            if (prefetchWindowId == windowId) {
                prefetchWindowId = null
                prefetchJob = null
            }
        }
    }

    private fun isWindowWithinDuration(windowId: Long): Boolean {
        if (windowId <= 0L) return false
        if (currentDurationMs <= 0L) return true
        return danmakuWindowStartMs(windowId) < currentDurationMs
    }

    private fun ensureWindowLoaded(windowId: Long) {
        if (_state.value.window?.id == windowId) return
        if (loadingWindowId == windowId && loadingJob?.isActive == true) return
        buildRequest(windowId)?.let(::loadWindow)
    }

    private fun buildRequest(windowId: Long): VodDanmakuRequest? {
        val ids = currentIds ?: return null
        val startMs = danmakuWindowStartMs(windowId)
        val positionMs = if (currentDurationMs > 0L) {
            startMs.coerceAtMost((currentDurationMs - 1L).coerceAtLeast(0L))
        } else {
            startMs
        }
        return VodDanmakuRequest(
            ids = ids,
            positionMs = positionMs,
            durationMs = currentDurationMs
        )
    }

    private fun loadWindow(request: VodDanmakuRequest) {
        val windowId = request.segmentIndex
        if (loadingWindowId != windowId) {
            loadingJob?.cancel()
        }
        loadingWindowId = windowId
        clearError()

        loadingJob = scope.launch {
            runCatching {
                repository.fetchSegment(request)
            }.onSuccess { segment ->
                if (segment.request.ids != currentIds || windowId != currentWindowId) {
                    return@onSuccess
                }
                _state.update { state ->
                    state.copy(
                        window = DanmakuWindow(
                            id = windowId,
                            items = segment.items
                        ),
                        lastError = null
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (request.ids != currentIds || windowId != currentWindowId) {
                    return@onFailure
                }
                _state.update { it.copy(lastError = error.message ?: "Failed to load danmaku segment") }
            }
            if (loadingWindowId == windowId) {
                loadingWindowId = null
                loadingJob = null
            }
        }
    }

    fun clear() {
        reset()
    }

    private fun resetSource(
        ids: ResolvedVideoIds,
        durationMs: Long
    ) {
        loadingJob?.cancel()
        loadingJob = null
        loadingWindowId = null
        prefetchJob?.cancel()
        prefetchJob = null
        prefetchWindowId = null
        currentIds = ids
        currentDurationMs = durationMs.coerceAtLeast(0L)
        currentWindowId = null
        _state.value = DanmakuSessionState(sourceKey = ids.toDanmakuSourceKey())
    }

    private fun clearError() {
        _state.update { if (it.lastError != null) it.copy(lastError = null) else it }
    }

    private fun updateSourceKey(sourceKey: String) {
        _state.update { state ->
            if (state.sourceKey == sourceKey) state else state.copy(sourceKey = sourceKey)
        }
    }

    private fun reset() {
        currentIds = null
        currentDurationMs = 0L
        currentWindowId = null
        loadingJob?.cancel()
        loadingJob = null
        loadingWindowId = null
        prefetchJob?.cancel()
        prefetchJob = null
        prefetchWindowId = null
        _state.value = DanmakuSessionState()
    }
}

private fun ResolvedVideoIds.toDanmakuSourceKey(): String {
    return buildString {
        append("vod:")
        append(aid)
        append(':')
        append(cid)
        bvid?.takeIf(String::isNotBlank)?.let {
            append(':')
            append(it)
        }
    }
}
