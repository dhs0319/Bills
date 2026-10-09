package com.dhs0319.bills.infra.player.danmaku

import android.view.View
import com.dhs0319.bills.core.model.DanmakuConfig
import com.dhs0319.bills.core.model.DanmakuItem
import com.dhs0319.bills.core.model.DanmakuSessionState
import com.dhs0319.bills.core.model.toDanmakuWindowId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import master.flame.danmaku.api.DanmakuSegmentData
import master.flame.danmaku.api.SegmentDanmakuSession
import master.flame.danmaku.controller.IDanmakuView
import master.flame.danmaku.danmaku.model.android.DanmakuContext

class DanmakuOverlayState internal constructor(
    internal val danmakuView: View,
    private val danmakuCtrl: IDanmakuView,
    private val danmakuContext: DanmakuContext,
    private val timeProvider: DanmakuPlayerTimeProvider,
    private val session: SegmentDanmakuSession<DanmakuItem>
) {
    private val released = AtomicBoolean(false)
    private var lastSourceKey: String? = null
    private var pendingSeek = true
    private var lastConfig: DanmakuConfig? = null
    private var lastPlayState: DanmakuPlayState? = null
    private var lastPlaybackSpeed = 1f
    private var lastSeekEventId = 0L
    private var appliedWindowId: Long? = null
    private var appliedWindowItems: List<DanmakuItem>? = null
    private val itemMapper = DefaultDanmakuItemMapper()

    fun prepare() {
        if (released.get()) return
        session.prepare()
    }

    fun sync(
        danmakuState: DanmakuSessionState,
        config: DanmakuConfig,
        positionMs: Long,
        isPlaying: Boolean,
        speed: Float,
        seekEventId: Long,
        hasSource: Boolean
    ) {
        if (released.get()) return
        val clampedPositionMs = positionMs.coerceAtLeast(0L)
        val clampedSpeed = speed.coerceIn(0.25f, 3f)
        val requiredWindowId = clampedPositionMs.toDanmakuWindowId()

        syncSource(danmakuState.sourceKey)
        val hasSeek = consumeSeekEvent(seekEventId)
        if (hasSeek) {
            pendingSeek = true
        }
        val hasSpeedChange = updatePlaybackSpeed(clampedSpeed)

        applyConfig(config)
        syncWindow(
            danmakuState = danmakuState,
            targetWindowId = requiredWindowId,
            hasDiscontinuity = hasSeek
        )
        val positionSynced = config.enabled && hasSource &&
            syncPosition(
                positionMs = clampedPositionMs,
                windowReady = appliedWindowId == requiredWindowId
            )

        val canPlay = isPlaying && !pendingSeek
        syncClock(
            positionMs = clampedPositionMs,
            isPlaying = canPlay,
            speed = clampedSpeed,
            hasDiscontinuity = hasSeek || positionSynced,
            hasSpeedChange = hasSpeedChange,
            hasSource = hasSource
        )
        syncPlayback(
            enabled = config.enabled,
            hasSource = hasSource,
            isPlaying = canPlay
        )
    }

    fun syncLive(
        config: DanmakuConfig,
        isPlaying: Boolean,
        speed: Float,
        hasSource: Boolean
    ) {
        if (released.get()) return
        val clampedSpeed = speed.coerceIn(0.25f, 3f)
        val hasSpeedChange = updatePlaybackSpeed(clampedSpeed)
        applyConfig(config)
        val needStateOverride = hasSpeedChange ||
            !isPlaying ||
            !hasSource ||
            lastPlayState?.isPlaying != true
        if (needStateOverride) {
            timeProvider.overrideState(
                positionMs = timeProvider.getCurrentTimeMs(),
                isPlaying = isPlaying,
                speed = clampedSpeed
            )
        }
        syncPlayback(
            enabled = config.enabled,
            hasSource = hasSource,
            isPlaying = isPlaying
        )
    }

    private fun updatePlaybackSpeed(speed: Float): Boolean {
        val hasChanged = lastPlaybackSpeed != speed
        lastPlaybackSpeed = speed
        return hasChanged
    }

    private fun syncClock(
        positionMs: Long,
        isPlaying: Boolean,
        speed: Float,
        hasDiscontinuity: Boolean,
        hasSpeedChange: Boolean,
        hasSource: Boolean
    ) {
        val needsPositionAnchor = hasDiscontinuity ||
            !isPlaying ||
            !hasSource ||
            lastPlayState?.isPlaying != true
        val anchorMs = when {
            // Seek、起播和暂停优先使用实际位置，避免换源或续播时沿用旧时钟。
            needsPositionAnchor -> positionMs
            // 持续播放时仅变速：沿用推算位置，避免秒级采样使弹幕时间轴倒退。
            hasSpeedChange -> timeProvider.getCurrentTimeMs()
            else -> return
        }
        timeProvider.overrideState(anchorMs, isPlaying, speed)
    }

    private fun syncPlayback(
        enabled: Boolean,
        hasSource: Boolean,
        isPlaying: Boolean
    ) {
        val nextState = DanmakuPlayState(enabled, hasSource, isPlaying)
        if (lastPlayState == nextState) {
            return
        }
        lastPlayState = nextState

        if (!enabled || !hasSource) {
            danmakuView.visibility = View.INVISIBLE
            session.hide()
            session.pause()
            pendingSeek = true
            return
        }

        danmakuView.visibility = View.VISIBLE
        session.show()
        if (isPlaying) {
            session.resume()
        } else {
            session.pause()
        }
    }

    private fun syncPosition(
        positionMs: Long,
        windowReady: Boolean
    ): Boolean {
        // Seek 事件和换源都会先设置 pendingSeek；等待目标分段就绪后再定位。
        if (!pendingSeek || !windowReady) return false
        session.seekTo(positionMs)
        pendingSeek = false
        return true
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        appliedWindowId = null
        appliedWindowItems = null
        session.setPlayerTimeProvider(null)
        session.pause()
        timeProvider.release()
        thread(
            start = true,
            isDaemon = true,
            name = "DanmakuRelease"
        ) {
            session.release()
        }
    }

    fun clearLiveDanmakus() {
        if (released.get()) return
        lastSourceKey = null
        lastSeekEventId = 0L
        resetWindow()
    }

    fun appendDanmaku(item: DanmakuItem) {
        if (released.get()) return
        val danmaku = itemMapper.map(item, danmakuContext) ?: return
        danmaku.time = danmaku.time.coerceAtLeast(timeProvider.getCurrentTimeMs()) + LIVE_DANMAKU_LEAD_MS
        danmaku.priority = LIVE_DANMAKU_PRIORITY
        danmakuCtrl.addDanmaku(danmaku)
    }

    private fun syncSource(sourceKey: String?) {
        if (lastSourceKey == sourceKey) return

        lastSourceKey = sourceKey
        lastSeekEventId = 0L
        resetWindow()
    }

    private fun resetWindow() {
        pendingSeek = true
        appliedWindowId = null
        appliedWindowItems = null
        session.clearSegments()
    }

    private fun applyConfig(
        config: DanmakuConfig
    ) {
        if (lastConfig == config) return

        danmakuContext.applyConfig(config, lastConfig)
        lastConfig = config
    }

    private fun syncWindow(
        danmakuState: DanmakuSessionState,
        targetWindowId: Long,
        hasDiscontinuity: Boolean
    ) {
        val targetWindow = danmakuState.windowAt(targetWindowId)
        if (targetWindow == null) {
            if (hasDiscontinuity) {
                resetWindow()
            } else if (appliedWindowId == null) {
                pendingSeek = true
            }
            return
        }
        // 常规同步先比较引用；新列表比较完整内容，避免漏掉文字、颜色等变化。
        if (appliedWindowId == targetWindow.id &&
            (appliedWindowItems === targetWindow.items || appliedWindowItems == targetWindow.items)
        ) {
            appliedWindowItems = targetWindow.items
            return
        }
        val previousWindowId = appliedWindowId
        val isContinuousAdvance = !hasDiscontinuity &&
            !pendingSeek &&
            previousWindowId != null &&
            targetWindow.id == previousWindowId + 1L
        if (isContinuousAdvance) {
            // 连续推进到下一分段时只追加新分段，不能清空引擎的弹幕列表：
            // 引擎每帧都会从主列表重新取可见窗口，主列表被清空后屏上弹幕会立即消失。
            session.appendSegment(
                DanmakuSegmentData(targetWindow.id, targetWindow.items)
            )
        } else {
            session.replaceSegments(
                listOf(DanmakuSegmentData(targetWindow.id, targetWindow.items))
            )
        }
        appliedWindowId = targetWindow.id
        appliedWindowItems = targetWindow.items
    }

    private fun consumeSeekEvent(seekEventId: Long): Boolean {
        if (seekEventId == 0L || seekEventId == lastSeekEventId) {
            return false
        }
        lastSeekEventId = seekEventId
        return true
    }
}

private data class DanmakuPlayState(
    val enabled: Boolean,
    val hasSource: Boolean,
    val isPlaying: Boolean
)

private const val LIVE_DANMAKU_LEAD_MS = 1_200L
private const val LIVE_DANMAKU_PRIORITY: Byte = 1
