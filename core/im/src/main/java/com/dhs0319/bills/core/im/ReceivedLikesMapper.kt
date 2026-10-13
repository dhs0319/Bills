package com.dhs0319.bills.core.im

import com.bapis.bilibili.im.gateway.interfaces.v1.Cursor
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgFeedFilterType
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgFeedMsgCard
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgListReq
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgListRsp
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgTabType
import com.dhs0319.bills.core.common.media.httpsImageUrlOrNull
import com.dhs0319.bills.core.model.User
import com.dhs0319.bills.core.model.im.MsgFeedCursor
import com.dhs0319.bills.core.model.im.ReceivedLikeItem
import com.dhs0319.bills.core.model.im.ReceivedLikeType
import com.dhs0319.bills.core.model.im.ReceivedLikesPage

internal fun receivedLikesRequest(cursor: MsgFeedCursor?, pageSize: Long): MsgListReq =
    MsgListReq.newBuilder()
        .setTabType(MsgTabType.ReceiveLike)
        .setFilterType(MsgFeedFilterType.MsgFeed_All)
        .setPagesize(pageSize)
        .apply {
            cursor?.let {
                setCursor(
                    Cursor.newBuilder()
                        .setId(it.id)
                        .setTime(it.time)
                        .setReplymsgid(it.replymsgid)
                        .setIsEnd(it.isEnd)
                )
            }
        }
        .build()

internal fun MsgListRsp.toReceivedLikesPage(): ReceivedLikesPage = ReceivedLikesPage(
    items = (topCardsList + msgCardsList).mapNotNull { it.toReceivedLikeItem() }
        .distinctBy { it.key },
    cursor = if (hasCursor() && !limit.limit) {
        MsgFeedCursor(
            id = cursor.id,
            time = cursor.time,
            replymsgid = cursor.replymsgid,
            isEnd = cursor.isEnd
        )
    } else null,
    notice = limit.desc.takeIf { it.isNotBlank() }
)

private fun MsgFeedMsgCard.toReceivedLikeItem(): ReceivedLikeItem? {
    val (type, msg) = when {
        msgItem.hasLikeCard() -> ReceivedLikeType.LIKE to msgItem.likeCard.likeMsg
        msgItem.hasCoinCard() -> ReceivedLikeType.COIN to msgItem.coinCard.coinMsg
        msgItem.hasFavoriteCard() -> ReceivedLikeType.FAVORITE to msgItem.favoriteCard.favMsg
        else -> return null
    }
    val description = msg.richTextContent.paragraphsList.mapNotNull { paragraph ->
        if (!paragraph.hasText()) return@mapNotNull null
        paragraph.text.nodesList.joinToString("") { node ->
            node.rawText.ifBlank {
                when {
                    node.hasWord() -> node.word.words
                    node.hasLink() -> node.link.showText.words
                    else -> ""
                }
            }
        }.takeIf { it.isNotBlank() }
    }.joinToString("\n")
    return ReceivedLikeItem(
        msgId = msgId,
        msgTime = msgTime.takeIf { it > 0L } ?: msg.msgTime,
        type = type,
        users = msg.usersList.map { user ->
            User(
                mid = user.mid,
                name = user.nickname,
                avatar = user.avatar.receivedLikeImageUrl().orEmpty()
            )
        },
        count = msg.counts,
        description = description,
        coverText = msg.coverText,
        coverImage = msg.coverImage.receivedLikeImageUrl(),
        nativeUri = msg.nativeUri,
        itemId = msg.itemId,
        originId = msg.originId,
        bizType = msg.bizType
    )
}

private fun String.receivedLikeImageUrl(): String? =
    (if (startsWith("//")) "https:$this" else httpsImageUrlOrNull())
        ?.takeIf { it.isNotBlank() }
