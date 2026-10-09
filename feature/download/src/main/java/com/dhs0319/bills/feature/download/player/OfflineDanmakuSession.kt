package com.dhs0319.bills.feature.download.player

import com.dhs0319.bills.core.download.VideoDownloadRepository
import com.dhs0319.bills.core.model.DanmakuItem
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.DanmakuWindow
import com.dhs0319.bills.core.model.DownloadDanmakuCache
import com.dhs0319.bills.core.model.VideoDownloadTask
import com.dhs0319.bills.core.model.toDanmakuWindowId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class OfflineDanmakuSession(
    private val scope: CoroutineScope,
    private val getTask: suspend (Long) -> VideoDownloadTask?,
    private val loadDanmaku: suspend (Long) -> DownloadDanmakuCache?
) {
    constructor(scope: CoroutineScope, repository: VideoDownloadRepository) :
        this(scope, repository::getTask, repository::loadDanmaku)

    private val _state = MutableStateFlow(DanmakuSessionState())
    val state: StateFlow<DanmakuSessionState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var currentTaskId: Long? = null
    private var cachedWindows: Map<Long, DanmakuWindow> = emptyMap()
    private var currentSourceKey: String? = null
    private var currentWindowId: Long? = null
    private var currentPositionMs = 0L

    fun bind(taskId: Long, forceReload: Boolean = false) {
        if (!forceReload && currentTaskId == taskId && (loadJob?.isActive == true || currentSourceKey != null)) return
        val positionMs = if (currentTaskId == taskId) currentPositionMs else 0L
        clear()
        if (taskId <= 0L) return
        currentPositionMs = positionMs
        currentTaskId = taskId
        val job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                load(taskId)
            } finally {
                if (loadJob === currentCoroutineContext()[Job]) loadJob = null
            }
        }
        loadJob = job
        job.start()
    }

    fun onTick(positionMs: Long) {
        // 缓存尚未加载时也记录位置，完成后直接发布续播/seek 所在分段。
        currentPositionMs = positionMs.coerceAtLeast(0L)
        val sourceKey = currentSourceKey ?: return
        val windowId = currentPositionMs.toDanmakuWindowId()
        if (windowId == currentWindowId) return
        currentWindowId = windowId
        _state.value = DanmakuSessionState(
            sourceKey = sourceKey,
            // 空分段也是已就绪的数据，避免 seek 到无弹幕区间时一直等待。
            window = cachedWindows[windowId] ?: DanmakuWindow(windowId, emptyList())
        )
    }

    fun clear() {
        val previousJob = loadJob
        loadJob = null
        previousJob?.cancel()
        currentTaskId = null
        cachedWindows = emptyMap()
        currentSourceKey = null
        currentWindowId = null
        currentPositionMs = 0L
        _state.value = DanmakuSessionState()
    }

    private suspend fun load(taskId: Long) {
        var sourceKey = buildSourceKey(taskId, 0L, 0L)
        try {
            val task = getTask(taskId)
            sourceKey = buildSourceKey(taskId, task?.aid ?: 0L, task?.cid ?: 0L)
            val cache = loadDanmaku(taskId)
            val windows = withContext(Dispatchers.Default) {
                cache?.items?.toWindows().orEmpty()
            }
            currentCoroutineContext().ensureActive()
            cachedWindows = windows
            currentSourceKey = cache?.let { buildSourceKey(taskId, it.aid, it.cid) } ?: sourceKey
            onTick(currentPositionMs)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            currentCoroutineContext().ensureActive()
            _state.value = DanmakuSessionState(
                sourceKey = sourceKey,
                lastError = error.message ?: "加载离线弹幕失败"
            )
        }
    }

    private fun buildSourceKey(taskId: Long, aid: Long, cid: Long): String {
        return "download:$taskId:$aid:$cid"
    }

    private fun List<DanmakuItem>.toWindows(): Map<Long, DanmakuWindow> {
        // 先按分段分组，再在各分段内排序，避免对整部视频的弹幕做全量排序。
        return groupBy { it.progressMs.toLong().toDanmakuWindowId() }
            .mapValues { (windowId, items) ->
                DanmakuWindow(windowId, items.sortedBy { it.progressMs })
            }
    }
}
