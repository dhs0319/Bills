package com.dhs0319.bills.feature.download.player

import com.dhs0319.bills.core.download.VideoDownloadRepository
import com.dhs0319.bills.core.model.DanmakuItem
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.DanmakuWindow
import com.dhs0319.bills.core.model.toDanmakuWindowId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class OfflineDanmakuSession(
    private val scope: CoroutineScope,
    private val repository: VideoDownloadRepository
) {
    private val _state = MutableStateFlow(DanmakuSessionState())
    val state: StateFlow<DanmakuSessionState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var cachedWindows: Map<Long, DanmakuWindow> = emptyMap()
    private var currentSourceKey: String? = null
    private var currentWindowId: Long? = null

    fun bind(taskId: Long) {
        if (loadJob?.isActive == true) return
        loadJob = scope.launch {
            load(taskId)
        }
    }

    fun onTick(positionMs: Long) {
        val sourceKey = currentSourceKey ?: return
        val windowId = positionMs.coerceAtLeast(0L).toDanmakuWindowId()
        if (windowId == currentWindowId) return
        currentWindowId = windowId
        _state.value = DanmakuSessionState(
            sourceKey = sourceKey,
            window = cachedWindows[windowId],
            lastError = _state.value.lastError
        )
    }

    fun clear() {
        loadJob?.cancel()
        loadJob = null
        reset()
    }

    private suspend fun load(taskId: Long) {
        if (taskId <= 0L) {
            reset()
            return
        }
        val task = repository.getTask(taskId)
        val fallbackKey = buildSourceKey(
            taskId = taskId,
            aid = task?.aid ?: 0L,
            cid = task?.cid ?: 0L
        )
        val cache = runCatching {
            repository.loadDanmaku(taskId)
        }.getOrElse { error ->
            reset()
            _state.value = DanmakuSessionState(
                sourceKey = fallbackKey,
                lastError = error.message ?: "加载离线弹幕失败"
            )
            return
        } ?: run {
            _state.value = DanmakuSessionState(sourceKey = fallbackKey)
            return
        }

        val sourceKey = buildSourceKey(
            taskId = taskId,
            aid = cache.aid,
            cid = cache.cid
        )
        cachedWindows = withContext(Dispatchers.Default) {
            cache.items.toWindows()
        }
        currentSourceKey = sourceKey
        currentWindowId = null
        _state.value = DanmakuSessionState(
            sourceKey = sourceKey,
            window = cachedWindows[1L]
        )
        currentWindowId = _state.value.window?.id
    }

    private fun reset() {
        cachedWindows = emptyMap()
        currentSourceKey = null
        currentWindowId = null
        _state.value = DanmakuSessionState()
    }

    private fun buildSourceKey(
        taskId: Long,
        aid: Long,
        cid: Long
    ): String {
        return "download:$taskId:$aid:$cid"
    }

    private fun List<DanmakuItem>.toWindows(): Map<Long, DanmakuWindow> {
        val grouped = linkedMapOf<Long, MutableList<DanmakuItem>>()
        sortedBy { it.progressMs }.forEach { item ->
            val windowId = item.progressMs.coerceAtLeast(0).toLong().toDanmakuWindowId()
            grouped.getOrPut(windowId) { mutableListOf() }.add(item)
        }
        val windows = linkedMapOf<Long, DanmakuWindow>()
        grouped.forEach { (windowId, items) ->
            windows[windowId] = DanmakuWindow(
                id = windowId,
                items = items
            )
        }
        return windows
    }
}
