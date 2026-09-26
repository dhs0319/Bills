package com.dhs0319.bills.core.model.listen

import androidx.compose.runtime.Immutable

@Immutable
data class ListenItem(
    val itemType: Int,
    val oid: Long,
    val subId: Long,
    val title: String,
    val cover: String,
    val author: String,
    val authorMid: Long,
    val duration: Long,
    val statView: Int,
    val statReply: Int,
    val message: String
) {
    val identityKey: String = "${oid}_${itemType}_${subId}"
    val durationText: String = if (duration > 0L) {
        val seconds = duration % 60L
        "${duration / 60L}:${seconds.toString().padStart(2, '0')}"
    } else {
        ""
    }
}

data class ListenRcmdResult(
    val items: List<ListenItem>,
    val historyLen: Long,
    val hasMore: Boolean,
    val nextPageToken: String
)

data class ListenPlayInfo(
    val playable: Boolean,
    val audioUrl: String?,
    val durationMs: Long
)
