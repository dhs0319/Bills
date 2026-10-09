package com.dhs0319.bills.core.im

import android.content.Context
import androidx.core.content.edit
import com.bapis.bilibili.app.im.v1.CustomerId
import com.bapis.bilibili.app.im.v1.DeleteSessionReply
import com.bapis.bilibili.app.im.v1.DeleteSessionReq
import com.bapis.bilibili.app.im.v1.FoldId
import com.bapis.bilibili.app.im.v1.GroupId
import com.bapis.bilibili.app.im.v1.MsgSummary
import com.bapis.bilibili.app.im.v1.Offset
import com.bapis.bilibili.app.im.v1.PaginationParams
import com.bapis.bilibili.app.im.v1.PinSessionReply
import com.bapis.bilibili.app.im.v1.PinSessionReq
import com.bapis.bilibili.app.im.v1.PrivateId
import com.bapis.bilibili.app.im.v1.Session
import com.bapis.bilibili.app.im.v1.SessionFilterType
import com.bapis.bilibili.app.im.v1.SessionMainReply
import com.bapis.bilibili.app.im.v1.SessionMainReq
import com.bapis.bilibili.app.im.v1.SessionPageType
import com.bapis.bilibili.app.im.v1.SessionSecondaryReply
import com.bapis.bilibili.app.im.v1.SessionSecondaryReq
import com.bapis.bilibili.app.im.v1.SessionId
import com.bapis.bilibili.app.im.v1.SessionType
import com.bapis.bilibili.app.im.v1.SystemId
import com.bapis.bilibili.app.im.v1.UnPinSessionReply
import com.bapis.bilibili.app.im.v1.UnPinSessionReq
import com.bapis.bilibili.app.im.v1.Unread
import com.bapis.bilibili.app.im.v1.UnreadStyle
import com.bapis.bilibili.dagw.component.avatar.common.ResourceSource
import com.bapis.bilibili.dagw.component.avatar.v1.AvatarItem
import com.bapis.bilibili.im.interfaces.v1.EmotionInfo
import com.bapis.bilibili.im.interfaces.v1.ReqSendMsg
import com.bapis.bilibili.im.interfaces.v1.ReqSessionMsg
import com.bapis.bilibili.im.interfaces.v1.ReqUpdateAck
import com.bapis.bilibili.im.interfaces.v1.RspSendMsg
import com.bapis.bilibili.im.interfaces.v1.RspSessionMsg
import com.bapis.bilibili.im.type.Msg
import com.google.protobuf.MessageLite
import com.google.protobuf.Parser
import com.bapis.bilibili.im.gateway.interfaces.v1.Cursor
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgFeedFilterType
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgListReq
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgListRsp
import com.bapis.bilibili.im.gateway.interfaces.v1.MsgTabType
import com.dhs0319.bills.core.model.User
import com.dhs0319.bills.core.model.im.MsgFeedCursor
import com.dhs0319.bills.core.model.im.MsgFeedItem
import com.dhs0319.bills.core.model.im.MsgFeedPage
import com.dhs0319.bills.core.common.AuthProvider
import com.dhs0319.bills.core.common.media.httpsImageUrlOrNull
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.ImConversationPage
import com.dhs0319.bills.core.model.ImMessage
import com.dhs0319.bills.core.model.ImPage
import com.dhs0319.bills.core.model.ImPaginationOffset
import com.dhs0319.bills.core.model.ImPaginationParams
import com.dhs0319.bills.core.model.ImMsgType
import com.dhs0319.bills.core.model.ImSessionId
import com.dhs0319.bills.core.model.ImSessionItem
import com.dhs0319.bills.core.model.ImSessionTab
import com.dhs0319.bills.infra.grpc.BiliGrpcClient
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject
import java.util.Random
import java.util.UUID

@Singleton
class ImRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val grpcClient: BiliGrpcClient,
    private val authProvider: AuthProvider
) {

    private val _sessionListVersion = MutableStateFlow(0L)
    val sessionListVersion: StateFlow<Long> = _sessionListVersion.asStateFlow()

    fun notifySessionListChanged() {
        _sessionListVersion.update { it + 1L }
    }

    suspend fun fetchSessions(
        tab: ImSessionTab = ImSessionTab.DEFAULT,
        paginationParams: ImPaginationParams? = null
    ): ImPage {
        return when (tab) {
            ImSessionTab.DEFAULT,
            ImSessionTab.FOLLOW -> fetchSessionPage(
                tab = tab,
                endpoint = MAIN_ENDPOINT,
                requestBytes = SessionMainReq.newBuilder()
                    .setFilterType(tab.toFilterType())
                    .apply {
                        paginationParams?.toProto()?.let(::setPaginationParams)
                    }
                    .build()
                    .toByteArray(),
                parser = SessionMainReply.parser(),
                pagination = { it.paginationParams },
                sessions = { it.sessionsList }
            )
            ImSessionTab.STRANGER -> fetchSessionPage(
                tab = tab,
                endpoint = SECONDARY_ENDPOINT,
                requestBytes = SessionSecondaryReq.newBuilder()
                    .setPageType(tab.toPageType())
                    .apply {
                        paginationParams?.toProto()?.let(::setPaginationParams)
                    }
                    .build()
                    .toByteArray(),
                parser = SessionSecondaryReply.parser(),
                pagination = { it.paginationParams },
                sessions = { it.sessionsList }
            )
        }
    }

    suspend fun fetchConversation(
        talkerId: Long,
        sessionType: Int
    ): ImConversationPage {
        val reply = fetchSessionMsgs(
            talkerId = talkerId,
            sessionType = sessionType,
            beginSeqNo = null,
            endSeqNo = null,
            size = DEFAULT_PAGE_SIZE,
            order = ORDER_DESC
        )
        return withContext(Dispatchers.Default) {
            reply.toConversationPage()
        }
    }

    suspend fun fetchOlderMessages(
        talkerId: Long,
        sessionType: Int,
        beforeSeqNo: Long,
        size: Int = 20
    ): ImConversationPage {
        val reply = fetchSessionMsgs(
            talkerId = talkerId,
            sessionType = sessionType,
            beginSeqNo = 0L,
            endSeqNo = beforeSeqNo,
            size = size,
            order = ORDER_DESC
        )
        return withContext(Dispatchers.Default) {
            reply.toConversationPage()
        }
    }

    suspend fun sendConversationMessage(
        talkerId: Long,
        sessionType: Int,
        text: String
    ): ImMessage {
        val mid = authProvider.mid
        require(mid > 0L) { "请先登录" }
        val cliMsgId = buildCliMsgId()
        val req = ReqSendMsg.newBuilder()
            .setMsg(
                Msg.newBuilder()
                    .setSenderUid(mid)
                    .setReceiverType(sessionType)
                    .setReceiverId(talkerId)
                    .setCliMsgId(cliMsgId)
                    .setMsgType(ImMsgType.TEXT)
                    .setContent("""{"content":"$text"}""")
                    .setNewFaceVersion(1)
                    .build()
            )
            .setCookie("")
            .setCookie2("")
            .setErrorCode(0)
            .setDevId(getDeviceId(authProvider.mid))
            .build()
        val reply = grpcClient.call(
            endpoint = SEND_MSG_ENDPOINT,
            requestBytes = req.toByteArray(),
            parser = RspSendMsg.parser()
        )
        notifySessionListChanged()
        return withContext(Dispatchers.Default) {
            reply.toSentMessage(
                talkerId = talkerId,
                fallbackText = text,
                cliMsgId = cliMsgId
            )
        }
    }

    suspend fun updateAck(
        talkerId: Long,
        sessionType: Int,
        ackSeqNo: Long
    ) {
        if (ackSeqNo <= 0L) return
        grpcClient.call(
            endpoint = UPDATE_ACK_ENDPOINT,
            requestBytes = ReqUpdateAck.newBuilder()
                .setTalkerId(talkerId)
                .setSessionType(sessionType)
                .setAckSeqno(ackSeqNo)
                .build()
                .toByteArray(),
            parser = com.bapis.bilibili.im.interfaces.v1.DummyRsp.parser()
        )
        notifySessionListChanged()
    }

    suspend fun setSessionPinned(sessionId: ImSessionId, pinned: Boolean) {
        if (pinned) {
            val req = PinSessionReq.newBuilder()
                .setSessionId(sessionId.toSessionIdProto())
                .setTopTimeMicros(System.currentTimeMillis() * 1000L)
                .build()
            grpcClient.call(
                endpoint = PIN_SESSION_ENDPOINT,
                requestBytes = req.toByteArray(),
                parser = PinSessionReply.parser()
            )
        } else {
            val req = UnPinSessionReq.newBuilder()
                .setSessionId(sessionId.toSessionIdProto())
                .build()
            grpcClient.call(
                endpoint = UNPIN_SESSION_ENDPOINT,
                requestBytes = req.toByteArray(),
                parser = UnPinSessionReply.parser()
            )
        }
        notifySessionListChanged()
    }

    suspend fun deleteSession(sessionId: ImSessionId) {
        val req = DeleteSessionReq.newBuilder()
            .setSessionId(sessionId.toSessionIdProto())
            .build()
        grpcClient.call(
            endpoint = DELETE_SESSION_ENDPOINT,
            requestBytes = req.toByteArray(),
            parser = DeleteSessionReply.parser()
        )
        notifySessionListChanged()
    }

    suspend fun fetchMsgFeedList(
        filterType: com.dhs0319.bills.core.model.MsgFeedFilter = com.dhs0319.bills.core.model.MsgFeedFilter.ALL,
        cursor: MsgFeedCursor? = null,
        pageSize: Long = 20
    ): MsgFeedPage {
        val mid = authProvider.mid
        require(mid > 0L) { "请先登录" }
        val req = MsgListReq.newBuilder()
            .setTabType(MsgTabType.ReceiveReply)
            .setFilterType(
                when (filterType) {
                    com.dhs0319.bills.core.model.MsgFeedFilter.ALL -> MsgFeedFilterType.MsgFeed_All
                    com.dhs0319.bills.core.model.MsgFeedFilter.AT_ME -> MsgFeedFilterType.MsgFeed_AtMe
                }
            )
            .setPagesize(pageSize)
        
        cursor?.let {
            req.cursor = Cursor.newBuilder()
                .setId(it.id)
                .setTime(it.time)
                .setReplymsgid(it.replymsgid)
                .setIsEnd(it.isEnd)
                .build()
        }

        val reply = grpcClient.call(
            endpoint = MSG_FEED_LIST_ENDPOINT,
            requestBytes = req.build().toByteArray(),
            parser = MsgListRsp.parser()
        )
        return withContext(Dispatchers.Default) {
            val mappedCards = reply.msgCardsList.mapNotNull { card ->
                val type = card.msgType.name
                val id = card.msgId
                val time = card.msgTime
                val item = card.msgItem
                if (item.hasReplyCard()) {
                    val replyCard = item.replyCard
                    val msg = replyCard.replyMsg
                    val biz = replyCard.replyBiz
                    MsgFeedItem(
                        msgId = id,
                        msgTime = time,
                        msgType = type,
                        users = msg.usersList.map { u ->
                            User(
                                mid = u.mid,
                                name = u.nickname,
                                avatar = u.avatar.httpsImageUrlOrNull() ?: ""
                            )
                        },
                        coverImage = msg.coverImage.httpsImageUrlOrNull(),
                        subjectId = android.net.Uri.parse(msg.nativeUri).lastPathSegment?.toLongOrNull() ?: 0L,
                        rootId = biz.rootId,
                        sourceId = biz.sourceId,
                        businessId = msg.businessId,
                        sourceContent = biz.sourceContent,
                        rootReplyContent = biz.rootReplyContent,
                        targetReplyContent = biz.targetReplyContent
                    )
                } else if (item.hasAtCard()) {
                    val atCard = item.atCard
                    val msg = atCard.atMsg
                    val biz = atCard.atBiz
                    MsgFeedItem(
                        msgId = id,
                        msgTime = time,
                        msgType = type,
                        users = msg.usersList.map { u ->
                            User(
                                mid = u.mid,
                                name = u.nickname,
                                avatar = u.avatar.httpsImageUrlOrNull() ?: ""
                            )
                        },
                        coverImage = msg.coverImage.httpsImageUrlOrNull(),
                        subjectId = biz.subjectId,
                        rootId = biz.rootId,
                        sourceId = biz.sourceId,
                        businessId = msg.businessId,
                        sourceContent = biz.sourceContent,
                        rootReplyContent = "",
                        targetReplyContent = ""
                    )
                } else null
            }
            MsgFeedPage(
                cursor = if (reply.hasCursor()) {
                    val c = reply.cursor
                    MsgFeedCursor(
                        id = c.id,
                        time = c.time,
                        replymsgid = c.replymsgid,
                        isEnd = c.isEnd
                    )
                } else null,
                msgCards = mappedCards
            )
        }
    }

    private suspend fun fetchSessionMsgs(
        talkerId: Long,
        sessionType: Int,
        beginSeqNo: Long?,
        endSeqNo: Long?,
        size: Int,
        order: Int
    ): RspSessionMsg {
        val req = ReqSessionMsg.newBuilder()
            .setTalkerId(talkerId)
            .setSessionType(sessionType)
            .setSize(size)
            .setOrder(order)
            .setDevId(getDeviceId(authProvider.mid, isFetch = true))
            .apply {
                beginSeqNo?.let(::setBeginSeqno)
                endSeqNo?.let(::setEndSeqno)
            }
            .build()
        return grpcClient.call(
            endpoint = FETCH_SESSION_MSGS_ENDPOINT,
            requestBytes = req.toByteArray(),
            parser = RspSessionMsg.parser()
        )
    }

    private suspend fun <Resp : MessageLite> fetchSessionPage(
        tab: ImSessionTab,
        endpoint: String,
        requestBytes: ByteArray,
        parser: Parser<Resp>,
        pagination: (Resp) -> PaginationParams,
        sessions: (Resp) -> List<Session>
    ): ImPage {
        val reply = grpcClient.call(
            endpoint = endpoint,
            requestBytes = requestBytes,
            parser = parser
        )
        return withContext(Dispatchers.Default) {
            ImPage(
                tabs = IM_TABS,
                currentTab = tab,
                paginationParams = pagination(reply).toModel(),
                sessions = sessions(reply).map(::mapSession)
            )
        }
    }

    private fun mapSession(session: Session): ImSessionItem {
        val talkerId = when (session.id.idCase) {
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.PRIVATE_ID -> session.id.privateId.talkerUid
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.GROUP_ID -> session.id.groupId.groupId
            else -> null
        }
        return ImSessionItem(
            key = buildKey(session, talkerId),
            talkerId = talkerId,
            sessionType = session.toLegacyConversationType(),
            sessionTypeLabel = sessionTypeLabel(session),
            name = session.sessionInfo.sessionName.ifBlank { "未命名会话" },
            avatar = session.sessionInfo.avatar.avatarUrl(),
            summary = session.msgSummary.summaryText(),
            latestMessageIsAutoReply = session.msgSummary.isAutoReplySummary(),
            unreadText = session.unread.unreadText(),
            unreadCount = session.unread.number,
            timeMicros = session.timestamp,
            isPinned = session.isPinned,
            isMuted = session.isMuted,
            sessionId = session.toModelSessionId(),
            canPin = session.canPin(),
            canUnpin = session.canUnpin(),
            canDelete = session.canDelete()
        )
    }

    private fun Session.toModelSessionId(): ImSessionId? = when (id.idCase) {
        SessionId.IdCase.PRIVATE_ID -> ImSessionId.Private(id.privateId.talkerUid)
        SessionId.IdCase.GROUP_ID -> ImSessionId.Group(id.groupId.groupId)
        SessionId.IdCase.FOLD_ID -> ImSessionId.Fold(id.foldId.typeValue)
        SessionId.IdCase.SYSTEM_ID -> ImSessionId.System(id.systemId.typeValue)
        SessionId.IdCase.CUSTOMER_ID -> ImSessionId.Customer(
            shopId = id.customerId.shopId,
            shopType = id.customerId.shopType
        )
        else -> null
    }

    private fun Session.canPin(): Boolean = !hasOperation() || operation.pin.show

    private fun Session.canUnpin(): Boolean = !hasOperation() || operation.unpin.show

    private fun Session.canDelete(): Boolean = !hasOperation() || operation.delete.show

    private fun ImSessionId.toSessionIdProto(): SessionId = when (this) {
        is ImSessionId.Private -> SessionId.newBuilder()
            .setPrivateId(PrivateId.newBuilder().setTalkerUid(talkerUid))
            .build()
        is ImSessionId.Group -> SessionId.newBuilder()
            .setGroupId(GroupId.newBuilder().setGroupId(groupId))
            .build()
        is ImSessionId.Fold -> SessionId.newBuilder()
            .setFoldId(FoldId.newBuilder().setTypeValue(type))
            .build()
        is ImSessionId.System -> SessionId.newBuilder()
            .setSystemId(SystemId.newBuilder().setTypeValue(type))
            .build()
        is ImSessionId.Customer -> SessionId.newBuilder()
            .setCustomerId(CustomerId.newBuilder().setShopId(shopId).setShopType(shopType))
            .build()
    }

    private fun mapMessage(msg: Msg): ImMessage {
        val senderUid = msg.senderUid
        val receiverId = if (msg.receiverType == CONVERSATION_TYPE_SINGLE && senderUid != authProvider.mid) {
            senderUid
        } else {
            msg.receiverId
        }
        val content = msg.parseContent()
        return ImMessage(
            key = msg.msgKey,
            seqNo = msg.msgSeqno,
            senderUid = senderUid,
            receiverId = receiverId,
            msgType = msg.msgType,
            content = content.text,
            imageUrl = content.imageUrl,
            imageWidth = content.imageWidth,
            imageHeight = content.imageHeight,
            timestampSec = msg.timestamp,
            isSelf = senderUid == authProvider.mid,
            isRecalled = msg.sysCancel || msg.msgStatus == RECALL_MSG_STATUS,
            isAutoReply = msg.hasKeyHitInfos(),
            shareCoverUrl = content.shareCoverUrl,
            shareViewCount = content.shareViewCount,
            shareAid = content.shareAid,
            noticeTitle = content.noticeTitle,
            noticeText = content.noticeText,
            noticeActionText = content.noticeActionText,
            noticeCoverUrl = content.noticeCoverUrl,
            noticeDetailText = content.noticeDetailText
        )
    }

    private fun Msg.parseContent(): ImMessageContent {
        if (content.isBlank()) return ImMessageContent("")
        return when (msgType) {
            ImMsgType.TEXT -> ImMessageContent(JSONObject(content).optString("content"))
            ImMsgType.IMAGE -> content.readImageContent()
            in ImMsgType.SHARE_TYPES -> content.readVideoCardContent()
            ImMsgType.NOTICE,
            ImMsgType.SYSTEM_NOTICE -> content.readNoticeContent()
            else -> ImMessageContent("")
        }
    }

    private fun String.readImageContent(): ImMessageContent {
        val obj = JSONObject(this)
        return ImMessageContent(
            text = "",
            imageUrl = obj.optString("url").takeIf(String::isNotBlank),
            imageWidth = obj.optInt("width"),
            imageHeight = obj.optInt("height")
        )
    }

    private fun String.readVideoCardContent(): ImMessageContent {
        val obj = JSONObject(this)
        return ImMessageContent(
            text = obj.optString("title"),
            shareCoverUrl = obj.optString("cover").takeIf(String::isNotBlank)?.httpsImageUrlOrNull(),
            shareViewCount = obj.optLong("view"),
            shareAid = obj.optLong("rid")
        )
    }

    private fun String.readNoticeContent(): ImMessageContent {
        val raw = trim()
        if (raw.isEmpty()) return ImMessageContent("")
        if (raw[0] == '[') return parseNoticeTextArray(raw)
        if (raw[0] != '{') return ImMessageContent("")

        val body = JSONObject(raw).optString("content").takeIf(String::isNotBlank)?.trim() ?: raw
        if (body[0] == '[') return parseNoticeTextArray(body)
        if (body[0] != '{') return ImMessageContent("")

        val obj = JSONObject(body)
        val title = obj.optString("title").takeIf(String::isNotBlank)
        val text = obj.optString("text").takeIf(String::isNotBlank)
        val actionText = obj.optString("jump_text").takeIf(String::isNotBlank)
        val coverUrl = obj.optString("cover").takeIf(String::isNotBlank)
            ?: obj.optJSONObject("biz_content")?.optString("backup_cover")?.takeIf(String::isNotBlank)
        val detailText = obj.optJSONArray("modules")?.let { modules ->
            buildString {
                for (i in 0 until modules.length()) {
                    val module = modules.optJSONObject(i) ?: continue
                    val mt = module.optString("title").takeIf(String::isNotBlank)
                    val md = module.optString("detail").takeIf(String::isNotBlank)
                    if (mt == null && md == null) continue
                    if (isNotEmpty()) append('\n')
                    mt?.let(::append)
                    if (mt != null && md != null) append(' ')
                    md?.let(::append)
                }
            }.takeIf(String::isNotBlank)
        }
        return ImMessageContent(
            text = title ?: text.orEmpty(),
            noticeTitle = title,
            noticeText = text,
            noticeActionText = actionText,
            noticeCoverUrl = coverUrl.httpsImageUrlOrNull(),
            noticeDetailText = detailText
        )
    }

    private fun parseNoticeTextArray(raw: String): ImMessageContent {
        val arr = JSONArray(raw)
        val text = buildString {
            for (i in 0 until arr.length()) {
                val t = arr.optJSONObject(i)?.optString("text")?.takeIf(String::isNotBlank) ?: continue
                if (isNotEmpty()) append('\n')
                append(t)
            }
        }
        return ImMessageContent(
            text = text,
            noticeText = text.takeIf(String::isNotBlank)
        )
    }

    /**
     * B 站服务端在 SyncFetchSessionMsgs 时，会自动过滤掉同 dev_id 发出的消息（默认客户端已有本地记录）。
     * 本项目无本地消息数据库，为避免自己刚发的消息被过滤，必须隔离发送和拉取的设备 ID：
     * - 发送消息：使用主设备 ID
     * - 拉取消息：使用独立的伪造设备 ID，伪装成多端拉取。
     */
    private fun getDeviceId(mid: Long, isFetch: Boolean = false): String {
        val prefs = context.getSharedPreferences("IMFieldsCache$mid", Context.MODE_PRIVATE)
        val key = if (isFetch) "key_fetch_device_id_v2" else "key_device_id_v2"
        return prefs.getString(key, null) ?: UUID.randomUUID().toString().also {
            prefs.edit { putString(key, it) }
        }
    }

    private fun buildCliMsgId(): Long {
        return Random(System.nanoTime()).nextInt(Int.MAX_VALUE - 1).toLong() + 1L
    }

    private data class ImMessageContent(
        val text: String,
        val imageUrl: String? = null,
        val imageWidth: Int = 0,
        val imageHeight: Int = 0,
        val shareCoverUrl: String? = null,
        val shareViewCount: Long = 0L,
        val shareAid: Long = 0L,
        val noticeTitle: String? = null,
        val noticeText: String? = null,
        val noticeActionText: String? = null,
        val noticeCoverUrl: String? = null,
        val noticeDetailText: String? = null
    )

    private fun RspSessionMsg.toMessages(): List<ImMessage> {
        return messagesList
            .filterNot { it.msgType == RECALL_MSG_TYPE && it.msgStatus != RECALL_MSG_STATUS }
            .map(::mapMessage)
    }

    private fun RspSessionMsg.toConversationPage(): ImConversationPage {
        return ImConversationPage(
            messages = toMessages(),
            hasMoreHistory = hasMore == 1,
            emotes = eInfosList.mapNotNull { it.toEmote() }
        )
    }

    private fun EmotionInfo.toEmote(): CommentEmote? {
        val token = text.takeIf(String::isNotBlank) ?: return null
        val rawUrl = url.takeIf(String::isNotBlank)
            ?: gifUrl.takeIf(String::isNotBlank)
            ?: return null
        return CommentEmote(
            text = token,
            url = rawUrl.httpsImageUrlOrNull() ?: return null,
            size = size.toLong().coerceAtLeast(1L)
        )
    }

    private fun RspSendMsg.toSentMessage(
        talkerId: Long,
        fallbackText: String,
        cliMsgId: Long
    ): ImMessage {
        return ImMessage(
            key = msgKey.takeIf { it > 0L } ?: cliMsgId,
            seqNo = seqno.takeIf { it > 0L } ?: cliMsgId,
            senderUid = authProvider.mid,
            receiverId = talkerId,
            msgType = ImMsgType.TEXT,
            content = fallbackText,
            timestampSec = System.currentTimeMillis() / 1000L,
            isSelf = true,
            isRecalled = false
        )
    }

    private fun buildKey(
        session: Session,
        talkerId: Long?
    ): String {
        return when (session.id.idCase) {
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.PRIVATE_ID -> "private:${talkerId ?: 0L}"
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.GROUP_ID -> "group:${session.id.groupId.groupId}"
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.FOLD_ID -> "fold:${session.id.foldId.typeValue}"
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.SYSTEM_ID -> "system:${session.id.systemId.typeValue}"
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.CUSTOMER_ID -> {
                "customer:${session.id.customerId.shopType}:${session.id.customerId.shopId}"
            }
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.ID_NOT_SET,
            null -> "unknown:${session.sequenceNumber}:${session.timestamp}"
        }
    }

    private fun Session.toLegacyConversationType(): Int? {
        return when (id.idCase) {
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.PRIVATE_ID -> CONVERSATION_TYPE_SINGLE
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.GROUP_ID -> CONVERSATION_TYPE_GROUP
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.CUSTOMER_ID -> CONVERSATION_TYPE_CUSTOMER
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.SYSTEM_ID -> when (id.systemId.type) {
                SessionType.SESSION_TYPE_UNFOLLOWED -> CONVERSATION_TYPE_UNFOLLOW
                SessionType.SESSION_TYPE_STRANGER -> CONVERSATION_TYPE_STRANGER
                SessionType.SESSION_TYPE_GROUP_FOLD -> CONVERSATION_TYPE_GROUP_FOLD
                SessionType.SESSION_TYPE_AI_FOLD -> CONVERSATION_TYPE_AI
                else -> null
            }
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.FOLD_ID -> when (id.foldId.type) {
                SessionType.SESSION_TYPE_UNFOLLOWED -> CONVERSATION_TYPE_UNFOLLOW
                SessionType.SESSION_TYPE_STRANGER -> CONVERSATION_TYPE_STRANGER
                SessionType.SESSION_TYPE_GROUP_FOLD -> CONVERSATION_TYPE_GROUP_FOLD
                SessionType.SESSION_TYPE_AI_FOLD -> CONVERSATION_TYPE_AI
                else -> null
            }
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.ID_NOT_SET,
            null -> null
        }
    }

    private fun sessionTypeLabel(session: Session): String? {
        val type = when (session.id.idCase) {
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.SYSTEM_ID -> session.id.systemId.type
            com.bapis.bilibili.app.im.v1.SessionId.IdCase.FOLD_ID -> session.id.foldId.type
            else -> SessionType.SESSION_TYPE_UNKNOWN
        }
        return when (type) {
            SessionType.SESSION_TYPE_SYSTEM -> "系统"
            SessionType.SESSION_TYPE_GROUP,
            SessionType.SESSION_TYPE_GROUP_FOLD -> "群聊"
            SessionType.SESSION_TYPE_CUSTOMER_ACCOUNT,
            SessionType.SESSION_TYPE_CUSTOMER_FOLD -> "客服"
            SessionType.SESSION_TYPE_AI_FOLD -> "AI"
            else -> null
        }
    }

    private fun MsgSummary.summaryText(): String {
        val prefix = prefixText.ifBlank { "" }
        val body = rawMsg.ifBlank { "暂无消息" }
        return if (prefix.isBlank()) body else "$prefix $body"
    }

    private fun MsgSummary.isAutoReplySummary(): Boolean {
        return summaryText().startsWith(AUTO_REPLY_MARKER)
    }

    private fun Unread.unreadText(): String? {
        if (style == UnreadStyle.UNREAD_STYLE_DOT) return "•"
        if (number <= 0L) return null
        return numberShow.ifBlank {
            if (number > MAX_UNREAD_COUNT) {
                "${MAX_UNREAD_COUNT}+"
            } else {
                number.toString()
            }
        }
    }

    private fun AvatarItem.avatarUrl(): String? {
        layersList.firstNotNullOfOrNull { it.imageUrl() }?.let { return it }
        fallbackLayers.imageUrl()?.let { return it }
        return null
    }

    private fun com.bapis.bilibili.dagw.component.avatar.v1.LayerGroup.imageUrl(): String? {
        return layersList.firstNotNullOfOrNull { layer ->
            val resource = layer.resource
            if (resource.resType != com.bapis.bilibili.dagw.component.avatar.v1.BasicLayerResource.ResType.RES_TYPE_IMAGE) {
                return@firstNotNullOfOrNull null
            }
            val imageSrc = resource.resImage.imageSrc
            if (imageSrc.srcType != ResourceSource.SourceType.SRC_TYPE_URL) {
                return@firstNotNullOfOrNull null
            }
            imageSrc.remote.url.takeIf { it.isNotBlank() }.httpsImageUrlOrNull()
        }
    }

    private fun ImSessionTab.toFilterType(): SessionFilterType {
        return when (this) {
            ImSessionTab.DEFAULT -> SessionFilterType.FILTER_DEFAULT
            ImSessionTab.FOLLOW -> SessionFilterType.FILTER_FOLLOW
            ImSessionTab.STRANGER -> error("陌生人页不走 SessionMain")
        }
    }

    private fun ImSessionTab.toPageType(): SessionPageType {
        return when (this) {
            ImSessionTab.STRANGER -> SessionPageType.SESSION_PAGE_TYPE_STRANGER
            else -> error("$title 不走 SessionSecondary")
        }
    }

    private fun PaginationParams.toModel(): ImPaginationParams {
        return ImPaginationParams(
            offsets = offsetsMap.mapValues { (_, offset) ->
                ImPaginationOffset(
                    normalOffset = offset.normalOffset,
                    topOffset = offset.topOffset
                )
            },
            hasMore = hasMore
        )
    }

    private fun ImPaginationParams.toProto(): PaginationParams {
        return PaginationParams.newBuilder()
            .putAllOffsets(
                offsets.mapValues { (_, offset) ->
                    Offset.newBuilder()
                        .setNormalOffset(offset.normalOffset)
                        .setTopOffset(offset.topOffset)
                        .build()
                }
            )
            .setHasMore(hasMore)
            .build()
    }

    private companion object {
        const val MAIN_ENDPOINT = "bilibili.app.im.v1.im/SessionMain"
        const val SECONDARY_ENDPOINT = "bilibili.app.im.v1.im/SessionSecondary"
        const val PIN_SESSION_ENDPOINT = "bilibili.app.im.v1.im/PinSession"
        const val UNPIN_SESSION_ENDPOINT = "bilibili.app.im.v1.im/UnpinSession"
        const val DELETE_SESSION_ENDPOINT = "bilibili.app.im.v1.im/DeleteSession"
        const val FETCH_SESSION_MSGS_ENDPOINT = "bilibili.im.interface.v1.ImInterface/SyncFetchSessionMsgs"
        const val SEND_MSG_ENDPOINT = "bilibili.im.interface.v1.ImInterface/SendMsg"
        const val UPDATE_ACK_ENDPOINT = "bilibili.im.interface.v1.ImInterface/UpdateAck"
        const val MSG_FEED_LIST_ENDPOINT = "bilibili.im.gateway.interface.v1.ImGatewayApi/MsgFeedMsgList"
        const val MAX_UNREAD_COUNT = 99
        const val DEFAULT_PAGE_SIZE = 20
        const val ORDER_DESC = 0
        const val CONVERSATION_TYPE_SINGLE = 1
        const val CONVERSATION_TYPE_GROUP = 2
        const val CONVERSATION_TYPE_UNFOLLOW = 102
        const val CONVERSATION_TYPE_GROUP_FOLD = 103
        const val CONVERSATION_TYPE_CUSTOMER = 106
        const val CONVERSATION_TYPE_AI = 107
        const val CONVERSATION_TYPE_STRANGER = 108
        const val RECALL_MSG_STATUS = 1
        const val RECALL_MSG_TYPE = 5
        const val AUTO_REPLY_MARKER = "[自动回复]"

        val IM_TABS = listOf(
            ImSessionTab.DEFAULT,
            ImSessionTab.FOLLOW,
            ImSessionTab.STRANGER
        )
    }
}
