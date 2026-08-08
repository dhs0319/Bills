package com.naaammme.bbspace.core.model

import androidx.compose.runtime.Immutable

const val PUBLISHED_RECORD_KIND_COMMENT = 1
const val PUBLISHED_RECORD_KIND_VIDEO_DANMAKU = 2
const val PUBLISHED_RECORD_KIND_LIVE_DANMAKU = 3

@Immutable
data class PublishedRecord(
    val key: String,
    val kind: Int,
    val itemId: Long,
    val targetId: Long,
    val targetType: Long,
    val senderMid: Long,
    val senderName: String,
    val senderAvatar: String,
    val content: String,
    val ctime: Long,
    val rootId: Long = 0L,
    val parentId: Long = 0L,
    val imageListJson: String? = null
)

object PublishedRecordKeyTool {
    fun comment(itemId: Long): String {
        return "comment:$itemId"
    }

    fun videoDanmaku(
        targetType: Long,
        targetId: Long,
        itemId: Long
    ): String {
        return "video_danmaku:$targetType:$targetId:$itemId"
    }

    fun liveDanmaku(
        roomId: Long,
        senderMid: Long,
        itemId: Long
    ): String {
        return "live_danmaku:$roomId:$senderMid:$itemId"
    }
}
