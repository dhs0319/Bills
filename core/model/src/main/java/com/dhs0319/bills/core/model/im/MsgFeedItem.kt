package com.dhs0319.bills.core.model.im

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.User

@Immutable
data class MsgFeedItem(
    val msgId: Long,
    val msgTime: Long,
    val msgType: String,
    val users: List<User>,
    val coverImage: String?,

    val subjectId: Long,
    val rootId: Long,
    val sourceId: Long,
    val businessId: Long,
    val sourceContent: String,
    val rootReplyContent: String,
    val targetReplyContent: String,
)

@Immutable
data class MsgFeedCursor(
    val id: Long,
    val time: Long,
    val replymsgid: Long,
    val isEnd: Boolean,
)

@Immutable
data class MsgFeedPage(
    val cursor: MsgFeedCursor?,
    val msgCards: List<MsgFeedItem>,
)
