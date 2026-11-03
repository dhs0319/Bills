package com.dhs0319.bills.feature.history.watchlater

import com.dhs0319.bills.feature.history.component.formatVideoDuration

internal data class WatchLaterProgress(
    val timeText: String,
    val fraction: Float?
)

internal fun watchLaterProgress(progressSec: Long?, durationSec: Long?): WatchLaterProgress? {
    if (progressSec == null || progressSec == 0L || progressSec < -1L) return null
    if (progressSec == -1L) return WatchLaterProgress(timeText = "已看完", fraction = 1f)

    val duration = durationSec?.takeIf { it > 0L }
        ?: return WatchLaterProgress(
            timeText = "已观看 ${formatVideoDuration(progressSec)}",
            fraction = null
        )
    if (progressSec >= duration) return WatchLaterProgress(timeText = "已看完", fraction = 1f)

    return WatchLaterProgress(
        timeText = "${formatVideoDuration(progressSec)}/${formatVideoDuration(duration)}",
        fraction = (progressSec.toFloat() / duration).coerceIn(0f, 1f)
    )
}
