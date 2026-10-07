package com.dhs0319.bills.feature.im.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.BiliImageVariant
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.designsystem.component.copyTextOnLongPress
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.ImMessage
import com.dhs0319.bills.core.model.ImMsgType
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoTargetTool
import com.dhs0319.bills.feature.comment.component.CommentRichText
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImConversationScreen(
    onBack: () -> Unit,
    onOpenSpace: ((SpaceRoute) -> Unit)? = null,
    onOpenVideo: ((VideoTarget) -> Unit)? = null,
    vm: ImConversationViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val rows = remember(state.messages) {
        state.messages.toConversationRows(now = ZonedDateTime.now())
    }
    val shouldLoadMore by remember(
        state.hasMoreHistory,
        state.isLoadingMore,
        rows.size,
        listState
    ) {
        androidx.compose.runtime.derivedStateOf {
            state.hasMoreHistory &&
                rows.isNotEmpty() &&
                !state.isLoadingMore &&
                listState.isScrollInProgress &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >=
                    listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }

    LaunchedEffect(state.lastSentMessageKey) {
        if (state.lastSentMessageKey != null) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title.ifBlank { "会话" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        },
        bottomBar = {
            ImConversationComposer(
                errorMessage = state.sendErrorMessage,
                onClearError = vm::clearSendError,
                onSend = { vm.sendMessage(it) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && rows.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize())
                }

                rows.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无消息",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        reverseLayout = true
                    ) {
                        items(
                            items = rows,
                            key = { it.key },
                            contentType = { it.contentType }
                        ) { row ->
                            when (row) {
                                is TimeDividerRow -> MessageTimeDivider(text = row.text)
                                is MessageRow -> ImMessageBubble(
                                    item = row.item,
                                    emotes = state.emotes,
                                    avatar = state.avatar,
                                    selfAvatar = state.selfAvatar,
                                    title = state.title,
                                    onOpenSpace = onOpenSpace,
                                    onOpenVideo = onOpenVideo
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImMessageBubble(
    item: ConversationMessageItem,
    emotes: List<CommentEmote>,
    avatar: String?,
    selfAvatar: String?,
    title: String?,
    onOpenSpace: ((SpaceRoute) -> Unit)?,
    onOpenVideo: ((VideoTarget) -> Unit)?
) {
    val message = item.message
    if (message.msgType == ImMsgType.SYSTEM_NOTICE) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SystemNoticeContent(item = item)
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start
    ) {
        val onOpenSenderSpace: (() -> Unit)? = if (onOpenSpace != null && message.senderUid > 0L) {
            { onOpenSpace(SpaceRoute(mid = message.senderUid)) }
        } else {
            null
        }
        if (message.isSelf) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                MessageContent(item = item, emotes = emotes, onOpenVideo = onOpenVideo)
                SenderAvatar(
                    url = selfAvatar,
                    contentDescription = "我的头像",
                    onClick = onOpenSenderSpace
                )
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                SenderAvatar(
                    url = avatar,
                    contentDescription = title ?: "头像",
                    onClick = onOpenSenderSpace
                )
                MessageContent(item = item, emotes = emotes, onOpenVideo = onOpenVideo)
            }
        }
    }
}

@Composable
private fun SenderAvatar(
    url: String?,
    contentDescription: String,
    onClick: (() -> Unit)?
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
    ) {
        AvatarImage(
            url = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MessageContent(
    item: ConversationMessageItem,
    emotes: List<CommentEmote>,
    onOpenVideo: ((VideoTarget) -> Unit)?
) {
    val message = item.message
    Column(
        horizontalAlignment = if (message.isSelf) Alignment.End else Alignment.Start
    ) {
        when {
            message.msgType == ImMsgType.NOTICE -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.widthIn(max = 260.dp)
                ) {
                    Column {
                        if (!message.noticeCoverUrl.isNullOrBlank()) {
                            CoverImage(
                                url = message.noticeCoverUrl,
                                contentDescription = message.noticeTitle ?: "通知",
                                shape = MaterialTheme.shapes.large,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                            )
                        }
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = message.noticeTitle ?: message.content.ifBlank { "通知" },
                                modifier = Modifier.copyTextOnLongPress(
                                    message.noticeTitle ?: message.content.ifBlank { "通知" },
                                    "消息"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            message.noticeText?.takeIf(String::isNotBlank)?.let { notice ->
                                Text(
                                    text = notice,
                                    modifier = Modifier.copyTextOnLongPress(notice, "消息"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            message.noticeDetailText?.takeIf(String::isNotBlank)?.let { detail ->
                                Text(
                                    text = detail,
                                    modifier = Modifier.copyTextOnLongPress(detail, "消息"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            message.noticeActionText?.takeIf(String::isNotBlank)?.let { action ->
                                HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
                                Text(
                                    text = action,
                                    modifier = Modifier.copyTextOnLongPress(action, "消息"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            !message.shareCoverUrl.isNullOrBlank() -> {
                val clickable = onOpenVideo != null && message.shareAid > 0L
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .widthIn(max = 240.dp)
                        .then(if (clickable) Modifier.clickable {
                            onOpenVideo!!(
                                VideoTarget.Ugc(
                                    aid = message.shareAid,
                                    cid = 0L,
                                    src = VideoTargetTool.default().copy(
                                        titleHint = message.content.takeIf(String::isNotBlank),
                                        coverHint = message.shareCoverUrl
                                    )
                                )
                            )
                        } else Modifier)
                ) {
                    val bodyColor = MaterialTheme.colorScheme.onSurface
                    val metaColor = bodyColor.copy(alpha = 0.58f)
                    Column {
                        CoverImage(
                            url = message.shareCoverUrl,
                            contentDescription = message.content.ifBlank { "视频卡片" },
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        )
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = message.content.ifBlank { "视频卡片" },
                                modifier = Modifier.copyTextOnLongPress(
                                    message.content.ifBlank { "视频卡片" },
                                    "消息"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = bodyColor,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${message.shareViewCount} 播放",
                                style = MaterialTheme.typography.labelSmall,
                                color = metaColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                RecallFlag(message.isRecalled)
            }

            !message.imageUrl.isNullOrBlank() -> {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                ) {
                    Box(modifier = Modifier.aspectRatio(item.imageRatio)) {
                        BiliAsyncImage(
                            url = message.imageUrl,
                            contentDescription = "图片消息",
                            modifier = Modifier.fillMaxSize(),
                            variant = BiliImageVariant.PreviewThumb,
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                RecallFlag(message.isRecalled)
            }

            else -> {
                val surfaceColor = if (message.isSelf) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                }
                val bodyColor = if (message.isSelf) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                val bubbleModifier = if (message.isAutoReply) {
                    Modifier.width(IntrinsicSize.Max)
                } else {
                    Modifier
                }
                Surface(
                    color = surfaceColor,
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 260.dp)
                            .then(bubbleModifier)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CommentRichText(
                            text = item.displayText,
                            emotes = emotes,
                            style = MaterialTheme.typography.bodyLarge.copy(color = bodyColor),
                            copyLabel = "消息"
                        )
                        if (message.isAutoReply) {
                            HorizontalDivider(modifier = Modifier.padding(top = 2.dp))
                            Text(
                                text = "此条消息为自动回复",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                RecallFlag(message.isRecalled)
            }
        }
    }
}

@Composable
private fun MessageTimeDivider(text: String) {
    if (text.isEmpty()) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SystemNoticeContent(item: ConversationMessageItem) {
    val message = item.message
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = message.noticeText ?: message.content,
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .copyTextOnLongPress(message.noticeText ?: message.content, "消息"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RecallFlag(
    isRecalled: Boolean
) {
    if (!isRecalled) return
    Text(
        text = "已撤回",
        modifier = Modifier.padding(top = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Immutable
private sealed interface ConversationRow {
    val key: Any
    val contentType: String
}

@Immutable
private data class MessageRow(val item: ConversationMessageItem) : ConversationRow {
    override val key: Any get() = item.message.key
    override val contentType: String get() = item.contentType
}

@Immutable
private data class TimeDividerRow(val messageKey: Long, val text: String) : ConversationRow {
    override val key: Any get() = "$CONTENT_TYPE_TIME$messageKey"
    override val contentType: String get() = CONTENT_TYPE_TIME
}

@Immutable
private data class ConversationMessageItem(
    val message: ImMessage,
    val displayText: String,
    val timeText: String,
    val imageRatio: Float,
    val contentType: String
)

private fun List<ImMessage>.toConversationRows(now: ZonedDateTime): List<ConversationRow> {
    val items = toConversationMessageItems(now)
    val rows = ArrayList<ConversationRow>(items.size + 4)
    items.forEachIndexed { index, item ->
        rows += MessageRow(item)
        val olderTimeText = items.getOrNull(index + 1)?.timeText
        if (item.timeText.isNotEmpty() && item.timeText != olderTimeText) {
            rows += TimeDividerRow(messageKey = item.message.key, text = item.timeText)
        }
    }
    return rows
}

private fun List<ImMessage>.toConversationMessageItems(now: ZonedDateTime): List<ConversationMessageItem> {
    return map { message ->
        ConversationMessageItem(
            message = message,
            displayText = message.displayText(),
            timeText = formatMessageTime(message.timestampSec, now),
            imageRatio = message.imageRatio(),
            contentType = when {
                message.msgType == ImMsgType.SYSTEM_NOTICE -> CONTENT_TYPE_SYSTEM_NOTICE
                message.msgType == ImMsgType.NOTICE -> CONTENT_TYPE_NOTICE
                !message.shareCoverUrl.isNullOrBlank() -> CONTENT_TYPE_SHARE
                !message.imageUrl.isNullOrBlank() -> CONTENT_TYPE_IMAGE
                else -> CONTENT_TYPE_TEXT
            }
        )
    }
}

private fun ImMessage.displayText(): String {
    content.takeIf(String::isNotBlank)?.let { return it }
    return when (msgType) {
        ImMsgType.IMAGE -> "[图片]"
        in ImMsgType.SHARE_TYPES -> "[分享]"
        else -> "[暂不支持的消息类型 $msgType]"
    }
}

private fun ImMessage.imageRatio(): Float {
    if (imageWidth <= 0 || imageHeight <= 0) return 1f
    return (imageWidth.toFloat() / imageHeight).coerceIn(0.45f, 1.8f)
}

private fun formatMessageTime(timestampSec: Long, now: ZonedDateTime): String {
    if (timestampSec <= 0L) return ""
    val time = Instant.ofEpochSecond(timestampSec).atZone(now.zone)
    val date = time.toLocalDate()
    return when {
        date == now.toLocalDate() -> time.format(TIME_FORMAT_HM)
        date == now.toLocalDate().minusDays(1L) -> "昨天 ${time.format(TIME_FORMAT_HM)}"
        date.year == now.year -> time.format(TIME_FORMAT_MD_HM)
        else -> time.format(TIME_FORMAT_YMD_HM)
    }
}

private val TIME_FORMAT_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val TIME_FORMAT_MD_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
private val TIME_FORMAT_YMD_HM: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")

private const val CONTENT_TYPE_SYSTEM_NOTICE = "system_notice"
private const val CONTENT_TYPE_NOTICE = "notice"
private const val CONTENT_TYPE_SHARE = "share"
private const val CONTENT_TYPE_TEXT = "text"
private const val CONTENT_TYPE_IMAGE = "image"
private const val CONTENT_TYPE_TIME = "time"

