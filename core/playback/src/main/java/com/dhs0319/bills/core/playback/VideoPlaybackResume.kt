package com.dhs0319.bills.core.playback

import com.dhs0319.bills.core.model.PlaybackHistory
import com.dhs0319.bills.core.model.PlaybackRequest
import com.dhs0319.bills.core.model.PlaybackSource
import com.dhs0319.bills.core.model.ResolvedVideoIds

internal fun resolveVideoStartPosition(
    request: PlaybackRequest,
    source: PlaybackSource,
    local: PlaybackHistory?
): Long {
    request.seekToMs?.let { return it.coerceAtLeast(0L) }
    val history = request.playable.resumePosition
    val positionMs: Long?
    val finished: Boolean
    // 历史页可能尚未刷新，也可能包含其他设备更新的进度，按记录时间选择。
    when {
        local != null && (history == null || local.updatedAt / 1000L >= history.updatedAtSec) -> {
            positionMs = local.progressMs
            finished = local.finished
        }
        history != null -> {
            positionMs = history.positionMs
            finished = false
        }
        else -> {
            positionMs = source.resumePositionMs
            finished = false
        }
    }
    if (positionMs == null || positionMs <= 0L || finished) return 0L
    if (source.durationMs > 0L && source.durationMs - positionMs <= 3_000L) return 0L
    return positionMs
}

internal fun PlaybackRequest.withResolvedVideoIds(resolved: ResolvedVideoIds): PlaybackRequest {
    return copy(
        playable = playable.copy(
            ids = ids.copy(
                aid = ids.aid.takeIf { it > 0L } ?: resolved.aid,
                cid = ids.cid.takeIf { it > 0L } ?: resolved.cid,
                epId = ids.epId.takeIf { it > 0L } ?: resolved.epId,
                seasonId = ids.seasonId.takeIf { it > 0L } ?: resolved.seasonId,
                bvid = ids.bvid?.takeIf(String::isNotBlank) ?: resolved.bvid
            )
        )
    )
}
