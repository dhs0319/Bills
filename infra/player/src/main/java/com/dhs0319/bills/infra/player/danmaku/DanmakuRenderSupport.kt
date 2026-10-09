package com.dhs0319.bills.infra.player.danmaku

import android.graphics.Color
import android.os.SystemClock
import com.dhs0319.bills.core.model.DanmakuConfig
import com.dhs0319.bills.core.model.DanmakuItem
import master.flame.danmaku.api.DanmakuItemMapper
import master.flame.danmaku.api.DanmakuItemUtils
import master.flame.danmaku.api.PlayerTimeProvider
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import kotlin.math.roundToInt

internal fun createDanmakuContext(
    density: Float
): DanmakuContext {
    return DanmakuContext.create()
        .setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, DANMAKU_STROKE_WIDTH)
        .setDanmakuMargin((density * DANMAKU_ROW_GAP_DP).roundToInt().coerceAtLeast(1))
}

internal class DefaultDanmakuItemMapper : DanmakuItemMapper<DanmakuItem> {

    override fun map(
        item: DanmakuItem,
        danmakuContext: DanmakuContext
    ): BaseDanmaku? {
        val text = item.content.trim()
        if (text.isEmpty()) {
            return null
        }

        val danmaku = DanmakuItemUtils.createTextDanmaku(
            danmakuContext,
            mapDanmakuType(item.mode),
            item.progressMs.toLong().coerceAtLeast(0L),
            text
        ) ?: return null

        danmaku.textColor = normalizeDanmakuColor(item.color)
        danmaku.textShadowColor = Color.BLACK
        danmaku.textSize = resolveDanmakuTextSize(
            fontSize = item.fontSize,
            density = danmakuContext.displayer.density
        )
        danmaku.priority = if (item.pool != 0) 1 else 0
        return danmaku
    }
}

internal class DanmakuPlayerTimeProvider(
    positionMs: Long = 0L,
    isPlaying: Boolean = false,
    speed: Float = 1f,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime
) : PlayerTimeProvider {
    // 绘制线程一次读取完整快照，避免混用新位置、旧时间或旧速度。
    @Volatile
    private var clock = ClockState(
        positionMs = positionMs.coerceAtLeast(0L),
        isPlaying = isPlaying,
        speed = speed.coerceAtLeast(0f),
        elapsedMs = elapsedRealtime()
    )

    fun overrideState(
        positionMs: Long,
        isPlaying: Boolean,
        speed: Float
    ) {
        clock = ClockState(
            positionMs = positionMs.coerceAtLeast(0L),
            isPlaying = isPlaying,
            speed = speed.coerceAtLeast(0f),
            elapsedMs = elapsedRealtime()
        )
    }

    fun release() {
    }

    override fun getCurrentTimeMs(): Long {
        val current = clock
        if (!current.isPlaying) return current.positionMs
        val deltaMs = (elapsedRealtime() - current.elapsedMs).coerceAtLeast(0L)
        return current.positionMs + (deltaMs * current.speed).toLong()
    }

    override fun isPlaying(): Boolean {
        return clock.isPlaying
    }

    override fun getSyncThresholdTimeMs(): Long {
        return DANMAKU_SEEK_SYNC_THRESHOLD_MS
    }

    private data class ClockState(
        val positionMs: Long,
        val isPlaying: Boolean,
        val speed: Float,
        val elapsedMs: Long
    )
}

private fun mapDanmakuType(mode: Int): Int {
    return when (mode) {
        BaseDanmaku.TYPE_FIX_BOTTOM -> BaseDanmaku.TYPE_FIX_BOTTOM
        BaseDanmaku.TYPE_FIX_TOP -> BaseDanmaku.TYPE_FIX_TOP
        BaseDanmaku.TYPE_SCROLL_LR -> BaseDanmaku.TYPE_SCROLL_LR
        else -> BaseDanmaku.TYPE_SCROLL_RL
    }
}

private fun normalizeDanmakuColor(color: Int): Int {
    return if (color ushr 24 == 0) {
        color or 0xFF000000.toInt()
    } else {
        color
    }
}

private fun resolveDanmakuTextSize(
    fontSize: Int,
    density: Float
): Float {
    return fontSize.coerceIn(18, 36).toFloat() * (density - 0.6f).coerceAtLeast(1f)
}

internal fun DanmakuContext.applyConfig(config: DanmakuConfig, previous: DanmakuConfig? = null) {
    if (previous?.opacity != config.opacity) setDanmakuTransparency(config.opacity)
    if (previous?.textScale != config.textScale) {
        setScaleTextSize(config.textScale.coerceIn(0.5f, 2f) * 0.6f)
    }
    if (previous?.speed != config.speed) {
        setScrollSpeedFactor(2f / config.speed.coerceIn(0.5f, 2f))
    }
    if (previous?.mergeDuplicates != config.mergeDuplicates) {
        setDuplicateMergingEnabled(config.mergeDuplicates)
    }
    if (previous?.densityLevel != config.densityLevel) {
        setMaximumVisibleSizeInScreen(config.maximumVisibleSize)
        preventOverlapping(config.overlappingRules)
    }
    if (previous?.areaPercent != config.areaPercent) setMaximumLines(config.maximumLines)
    if (previous?.showScrollRl != config.showScrollRl) setR2LDanmakuVisibility(config.showScrollRl)
    if (previous == null) setL2RDanmakuVisibility(true)
    if (previous?.showTop != config.showTop) setFTDanmakuVisibility(config.showTop)
    if (previous?.showBottom != config.showBottom) setFBDanmakuVisibility(config.showBottom)
}

private val DanmakuConfig.maximumVisibleSize: Int
    get() = when (densityLevel) {
        0 -> 20
        1 -> -1
        2 -> 0
        else -> -1
    }

private val DanmakuConfig.maximumLines: Map<Int, Int>
    get() {
        val lines = when (areaPercent) {
            25 -> 3
            50 -> 6
            75 -> 9
            else -> 12
        }
        return hashMapOf(
            BaseDanmaku.TYPE_SCROLL_RL to lines,
            BaseDanmaku.TYPE_SCROLL_LR to lines
        )
    }

private val DanmakuConfig.overlappingRules: Map<Int, Boolean>?
    get() = when (densityLevel) {
        0, 1 -> hashMapOf(
            BaseDanmaku.TYPE_SCROLL_RL to true,
            BaseDanmaku.TYPE_SCROLL_LR to true,
            BaseDanmaku.TYPE_FIX_TOP to true,
            BaseDanmaku.TYPE_FIX_BOTTOM to true
        )

        else -> null
    }

internal const val DANMAKU_SEEK_SYNC_THRESHOLD_MS = 1_000L
private const val DANMAKU_STROKE_WIDTH = 2f
private const val DANMAKU_ROW_GAP_DP = 4f
