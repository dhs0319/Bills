package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.danmaku.VodDanmakuRepository
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.DanmakuWindow
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.core.model.VodDanmakuRequest
import com.dhs0319.bills.core.model.VodDanmakuSegment
import com.dhs0319.bills.core.model.danmakuWindowStartMs
import com.dhs0319.bills.core.model.toDanmakuWindowId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class VodDanmakuSession(
    private val scope: CoroutineScope,
    private val fetchSegment: suspend (VodDanmakuRequest) -> VodDanmakuSegment,
    private val nanoTime: () -> Long = System::nanoTime
) {
    constructor(scope: CoroutineScope, repository: VodDanmakuRepository) :
        this(scope, repository::fetchSegment)

    private val _state = MutableStateFlow(DanmakuSessionState())
    val state: StateFlow<DanmakuSessionState> = _state.asStateFlow()

    // 当前分段和下一分段共用请求表；播放到边界时复用尚未完成的预取。
    private val loads = mutableMapOf<Long, Job>()
    private var currentIds: ResolvedVideoIds? = null
    private var currentDurationMs = 0L
    private var currentWindowId: Long? = null
    private var lastFailureNanos: Long? = null

    fun setSource(ids: ResolvedVideoIds?, durationMs: Long) {
        val validIds = ids?.takeIf { it.danmakuReady }
        if (currentIds != validIds) {
            clear()
            currentIds = validIds
            _state.value = DanmakuSessionState(sourceKey = validIds?.toDanmakuSourceKey())
        }
        currentDurationMs = if (validIds != null) durationMs.coerceAtLeast(0L) else 0L
    }

    fun seekTo(positionMs: Long) {
        currentWindowId = null
        ensureWindowAt(positionMs)
    }

    fun onProgress(positionMs: Long) {
        ensureWindowAt(positionMs)
    }

    private fun ensureWindowAt(positionMs: Long) {
        if (currentIds == null) return
        val position = if (currentDurationMs > 0L) {
            positionMs.coerceIn(0L, currentDurationMs - 1L)
        } else {
            positionMs.coerceAtLeast(0L)
        }
        val windowId = position.toDanmakuWindowId()
        if (windowId == currentWindowId) {
            val failedAt = lastFailureNanos ?: return
            if (nanoTime() - failedAt < RETRY_INTERVAL_NANOS) return
        }
        currentWindowId = windowId
        lastFailureNanos = null
        usePrefetchedWindow(windowId)
        cancelObsoleteLoads(windowId)
        ensureWindowLoaded(windowId)
        ensureWindowLoaded(windowId + 1L)
    }

    private fun usePrefetchedWindow(windowId: Long) {
        _state.update { state ->
            val prefetched = state.prefetchWindow
            when {
                prefetched?.id == windowId -> state.copy(window = prefetched, prefetchWindow = null, lastError = null)
                prefetched != null && prefetched.id != windowId + 1L -> state.copy(prefetchWindow = null)
                else -> state
            }
        }
    }

    private fun cancelObsoleteLoads(windowId: Long) {
        val obsoleteIds = loads.keys.filter { it != windowId && it != windowId + 1L }
        // 先移除再取消，避免旧协程的 finally 清理新请求。
        for (obsoleteId in obsoleteIds) {
            loads.remove(obsoleteId)?.cancel()
        }
    }

    private fun ensureWindowLoaded(windowId: Long) {
        if (_state.value.windowAt(windowId) != null || loads.containsKey(windowId)) return
        val request = buildRequest(windowId) ?: return
        if (windowId == currentWindowId) {
            _state.update { if (it.lastError == null) it else it.copy(lastError = null) }
        }
        // 先登记再启动，兼容 Main.immediate 上立即返回的缓存结果。
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val segment = fetchSegment(request)
                currentCoroutineContext().ensureActive()
                publishWindow(request, segment)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (request.ids == currentIds && windowId == currentWindowId) {
                    lastFailureNanos = nanoTime()
                    _state.update { it.copy(lastError = error.message ?: "Failed to load danmaku segment") }
                }
            } finally {
                if (loads[windowId] === currentCoroutineContext()[Job]) {
                    loads.remove(windowId)
                }
            }
        }
        loads[windowId] = job
        job.start()
    }

    private fun buildRequest(windowId: Long): VodDanmakuRequest? {
        val ids = currentIds ?: return null
        val startMs = danmakuWindowStartMs(windowId)
        if (currentDurationMs > 0L && startMs >= currentDurationMs) return null
        return VodDanmakuRequest(ids = ids, positionMs = startMs, durationMs = currentDurationMs)
    }

    private fun publishWindow(request: VodDanmakuRequest, segment: VodDanmakuSegment) {
        if (request.ids != currentIds) return
        val windowId = request.segmentIndex
        val current = currentWindowId ?: return
        val window = DanmakuWindow(windowId, segment.items)
        _state.update { state ->
            when (windowId) {
                current -> state.copy(window = window, lastError = null)
                current + 1L -> state.copy(prefetchWindow = window)
                else -> state
            }
        }
    }

    fun clear() {
        val obsoleteLoads = loads.values.toList()
        loads.clear()
        obsoleteLoads.forEach { it.cancel() }
        currentIds = null
        currentDurationMs = 0L
        currentWindowId = null
        lastFailureNanos = null
        _state.value = DanmakuSessionState()
    }

    private companion object {
        const val RETRY_INTERVAL_NANOS = 5_000_000_000L
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
