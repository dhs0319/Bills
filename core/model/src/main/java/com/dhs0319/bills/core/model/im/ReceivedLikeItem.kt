package com.dhs0319.bills.core.model.im

import androidx.compose.runtime.Immutable
import com.dhs0319.bills.core.model.User

enum class ReceivedLikeType {
    LIKE, COIN, FAVORITE
}

@Immutable
data class ReceivedLikeItem(
    val msgId: Long,
    val msgTime: Long,
    val type: ReceivedLikeType,
    val users: List<User>,
    val count: Long,
    val description: String,
    val coverText: String,
    val coverImage: String?,
    val nativeUri: String = "",
    val itemId: Long = 0L,
    val originId: Long = 0L,
    val bizType: String = ""
) {
    val key: String get() = "${type.name}:$msgId"
}

@Immutable
data class ReceivedLikesPage(
    val items: List<ReceivedLikeItem>,
    val cursor: MsgFeedCursor?,
    val notice: String? = null
)
