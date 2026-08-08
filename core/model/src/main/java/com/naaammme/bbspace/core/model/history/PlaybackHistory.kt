package com.naaammme.bbspace.core.model

import androidx.compose.runtime.Immutable
import java.util.Locale

@Immutable
data class PlaybackHistory(
    val uid: Long,
    val key: String,
    val biz: String,
    val aid: Long,
    val cid: Long,
    val epId: Long? = null,
    val seasonId: Long? = null,
    val durationMs: Long = 0L,
    val progressMs: Long = 0L,
    val watchMs: Long = 0L,
    val updatedAt: Long = 0L,
    val finished: Boolean = false
) {
    val id: String
        get() = PlaybackHistoryKey.videoId(uid, key)
}

object PlaybackHistoryKey {
    fun video(report: PlayReportParams): String {
        return video(
            biz = report.biz.name,
            aid = report.aid,
            cid = report.cid,
            epId = report.epId
        )
    }

    fun video(
        biz: String,
        aid: Long,
        cid: Long,
        epId: Long?
    ): String {
        val bizName = biz.lowercase(Locale.ROOT)
        val mainId = when {
            PlayBiz.PGC.name.equals(biz, ignoreCase = true) && (epId ?: 0L) > 0L -> epId ?: 0L
            else -> aid
        }
        return "$bizName:$mainId:$cid"
    }

    fun videoId(
        uid: Long,
        key: String
    ): String {
        return "$uid:$key"
    }
}
