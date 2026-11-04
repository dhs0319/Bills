package com.dhs0319.bills.feature.im.conversation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
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
import com.dhs0319.bills.core.model.ImMsgType
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoTargetTool
import com.dhs0319.bills.feature.comment.component.CommentRichText
import com.dhs0319.bills.feature.comment.component.CommentEmoteCatalog

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
    val rows = state.rows
    val emoteCatalog = remember(state.emotes) { CommentEmoteCatalog(state.emotes) }

    LaunchedEffect(listState, vm) {
        snapshotFlow {
            state.hasMoreHistory && state.rows.isNotEmpty() &&
                !state.isLoading && !state.isRefreshing && !state.isLoadingMore &&
                listState.isScrollInProgress &&
                (listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1) >=
                    listState.layoutInfo.totalItemsCount - 3
        }.collect { shouldLoadMore ->
            if (shouldLoadMore) vm.loadMore()
        }
    }

    LaunchedEffect(state.lastSentMessageKey) {
        if (state.lastSentMessageKey != null) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
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
            ImConversationComposerRoute(vm)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
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
                                    emoteCatalog = emoteCatalog,
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
private fun ImConversationComposerRoute(vm: ImConversationViewModel) {
    val state by vm.composerState.collectAsStateWithLifecycle()
    ImConversationComposer(
        state = state,
        onInputChange = vm::updateInput,
        onSelectImage = vm::selectImage,
        onReloadEmotes = { vm.loadEmotes(force = true) },
        onSelectEmotePackage = vm::selectEmotePackage,
        onInsertEmote = vm::insertEmote,
        onDeleteText = vm::deleteText,
        onSend = vm::sendMessage
    )
}

@Composable
private fun ImMessageBubble(
    item: ConversationMessageItem,
    emoteCatalog: CommentEmoteCatalog,
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
                MessageContent(item = item, emoteCatalog = emoteCatalog, onOpenVideo = onOpenVideo)
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
                MessageContent(item = item, emoteCatalog = emoteCatalog, onOpenVideo = onOpenVideo)
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
    emoteCatalog: CommentEmoteCatalog,
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
                            emotes = emptyList(),
                            emoteCatalog = emoteCatalog,
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
