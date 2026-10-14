package com.dhs0319.bills.feature.im.likes

import com.dhs0319.bills.core.model.CommentSubjectTool
import com.dhs0319.bills.core.model.PUBLISHED_RECORD_KIND_COMMENT
import com.dhs0319.bills.core.model.PublishedRecord
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.WebLinkParser
import com.dhs0319.bills.core.model.WebLinkTarget
import com.dhs0319.bills.core.model.im.ReceivedLikeItem
import java.net.URI
import java.net.URLDecoder

internal sealed interface ReceivedLikeDestination {
    data class Comment(val record: PublishedRecord) : ReceivedLikeDestination
    data class Content(val target: WebLinkTarget) : ReceivedLikeDestination
    data class Dynamic(val id: String) : ReceivedLikeDestination
}

internal fun ReceivedLikeItem.destination(): ReceivedLikeDestination? {
    val uri = runCatching { URI(nativeUri) }.getOrNull() ?: return null
    if (uri.scheme !in setOf("bilibili", "http", "https")) return null
    val query = runCatching {
        uri.rawQuery.orEmpty().split('&').filter { it.contains('=') }.associate {
            URLDecoder.decode(it.substringBefore('='), "UTF-8") to
                URLDecoder.decode(it.substringAfter('='), "UTF-8")
        }
    }.getOrNull() ?: return null
    fun parameter(name: String): Long? = query[name]?.toLongOrNull()?.takeIf { it > 0L }

    val contentTarget = WebLinkParser.parse(nativeUri)
    val dynamicId = uri.dynamicId()
    val rootId = parameter("comment_root_id")
    val replyId = parameter("comment_secondary_id")
        ?: itemId.takeIf { bizType == "reply" && it > 0L }
        ?: rootId
    if (bizType == "reply" || rootId != null || parameter("comment_secondary_id") != null) {
        val (subjectId, subjectType) = when {
            contentTarget is WebLinkTarget.ToVideo -> {
                val aid = when (val video = contentTarget.target) {
                    is VideoTarget.Ugc -> video.aid
                    is VideoTarget.Pgc -> video.aid
                    is VideoTarget.Pugv -> video.aid
                }
                (aid.takeIf { it > 0L } ?: originId) to CommentSubjectTool.TYPE_VIDEO
            }
            dynamicId != null -> dynamicId.toLong() to
                (parameter("comment_type") ?: CommentSubjectTool.TYPE_DYNAMIC)
            else -> return null
        }
        if (subjectId <= 0L || replyId == null) return null
        return ReceivedLikeDestination.Comment(
            PublishedRecord(
                key = "received_like:$key:$replyId",
                kind = PUBLISHED_RECORD_KIND_COMMENT,
                itemId = replyId,
                targetId = subjectId,
                targetType = subjectType,
                // The notification's users are the people who liked it, not the comment author.
                senderMid = 0L,
                senderName = "",
                senderAvatar = "",
                content = coverText,
                ctime = msgTime,
                rootId = rootId ?: replyId
            )
        )
    }
    if (dynamicId != null) return ReceivedLikeDestination.Dynamic(dynamicId)
    return when (contentTarget) {
        is WebLinkTarget.ToVideo, is WebLinkTarget.ToLive, is WebLinkTarget.ToSpace ->
            ReceivedLikeDestination.Content(contentTarget)
        WebLinkTarget.External, WebLinkTarget.Stay -> null
    }
}

private fun URI.dynamicId(): String? {
    val path = this.path.orEmpty().trimEnd('/')
    val isDynamic = when {
        scheme == "bilibili" ->
            (host == "following" && path.startsWith("/detail/")) || host == "opus" || host == "dynamic"
        scheme == "http" || scheme == "https" ->
            host == "t.bilibili.com" ||
                (host == "www.bilibili.com" && path.startsWith("/opus/"))
        else -> false
    }
    return path.substringAfterLast('/').takeIf { isDynamic && (it.toLongOrNull() ?: 0L) > 0L }
}
