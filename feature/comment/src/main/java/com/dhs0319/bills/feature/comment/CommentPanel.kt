package com.dhs0319.bills.feature.comment

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.common.log.Logger
import com.dhs0319.bills.core.designsystem.component.CommentCardSkeleton
import com.dhs0319.bills.core.designsystem.component.PreviewImage
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.model.PublishedRecord
import com.dhs0319.bills.core.model.CommentSubjectTool
import com.dhs0319.bills.core.model.CommentSort
import com.dhs0319.bills.core.model.CommentSubject
import com.dhs0319.bills.core.model.CommentUser
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.PUBLISHED_RECORD_KIND_COMMENT
import com.dhs0319.bills.core.model.PUBLISHED_RECORD_KIND_LIVE_DANMAKU
import com.dhs0319.bills.core.model.PUBLISHED_RECORD_KIND_VIDEO_DANMAKU
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoSrc
import com.dhs0319.bills.core.model.VideoTargetTool
import com.dhs0319.bills.feature.comment.component.CommentCard
import com.dhs0319.bills.feature.comment.component.CommentReplyAction
import com.dhs0319.bills.feature.comment.editor.CommentEditorFab
import com.dhs0319.bills.feature.comment.editor.CommentEditorSheet
import com.dhs0319.bills.feature.comment.thread.CommentThreadPane
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CommentPanel(
    subject: CommentSubject?,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    detailRecord: PublishedRecord? = null,
    onOpenSpace: (SpaceRoute) -> Unit = {},
    onOpenVideoDetail: (VideoTarget) -> Unit = {},
    onOpenDynamicDetail: (String) -> Unit = {},
    onOpenLiveDetail: (LiveRoute) -> Unit = {},
    onDismissDetail: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    threadListState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    header: (@Composable () -> Unit)? = null,
    viewModel: CommentViewModel = hiltViewModel()
) {
    LaunchedEffect(isActive, subject, detailRecord?.key) {
        if (isActive) {
            if (detailRecord != null) {
                viewModel.bindDetail(detailRecord)
            } else if (subject != null) {
                viewModel.bind(subject)
            }
        }
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDetailMode = detailRecord != null || (subject == null && uiState.threadPane != null)
    val layoutDirection = LocalLayoutDirection.current
    val replyThread = uiState.threadPane
    val isInitLoading = !isDetailMode && subject != null && uiState.loading && uiState.items.isEmpty()
    val context = LocalContext.current
    val appCtx = context.applicationContext
    val scope = rememberCoroutineScope()
    val listContentPadding = PaddingValues(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateEndPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding() + COMMENT_FAB_SPACE
    )

    var fabVisible by remember { mutableStateOf(true) }
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            !isDetailMode &&
                uiState.threadPane == null &&
                uiState.hasMore &&
                !uiState.loading &&
                !uiState.loadingMore &&
                uiState.loadMoreError.isNullOrBlank() &&
                uiState.items.isNotEmpty() &&
                total > 0 &&
                last >= total - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    LaunchedEffect(replyThread != null) {
        fabVisible = true
    }

    LaunchedEffect(listState, threadListState, replyThread != null) {
        val activeListState = if (replyThread != null) threadListState else listState
        var lastPos = activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
        snapshotFlow {
            activeListState.firstVisibleItemIndex to activeListState.firstVisibleItemScrollOffset
        }
            .collectLatest { index ->
                if (index != lastPos) {
                    fabVisible = index.first < lastPos.first ||
                        (index.first == lastPos.first && index.second <= lastPos.second)
                    lastPos = index
                }
            }
    }

    LaunchedEffect(viewModel, context) {
        viewModel.msg.collectLatest { text ->
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler(enabled = replyThread != null && !isDetailMode) {
        viewModel.closeReplyThread()
    }

    val routeSubject = uiState.subject ?: subject
    val onReplyAction: (CommentReplyAction) -> Unit = remember(
        routeSubject,
        detailRecord?.key,
        onOpenSpace,
        onOpenLiveDetail,
        onOpenVideoDetail,
        onOpenDynamicDetail
    ) {
        { action ->
            when (action) {
                is CommentReplyAction.Check -> viewModel.checkReply(action.rpid)
                is CommentReplyAction.Translate -> viewModel.translateReply(action.rpid)
                is CommentReplyAction.Delete -> viewModel.deleteReply(action.reply)
                is CommentReplyAction.Reply -> viewModel.replyTo(action.reply)
                is CommentReplyAction.OpenReplies -> viewModel.openReplyThread(action.reply)
                is CommentReplyAction.OpenUser -> {
                    action.user.toSpaceRoute(routeSubject)?.let(onOpenSpace)
                }
                is CommentReplyAction.OpenOriginalContent -> {
                    val oid = detailRecord?.targetId ?: routeSubject?.oid ?: 0L
                    val type = detailRecord?.targetType ?: routeSubject?.type ?: 0
                    val kind = detailRecord?.kind
                    if (oid > 0L) {
                        when {
                            kind == PUBLISHED_RECORD_KIND_LIVE_DANMAKU -> {
                                onOpenLiveDetail(LiveRoute(roomId = oid))
                            }
                            kind == PUBLISHED_RECORD_KIND_VIDEO_DANMAKU ||
                            (kind == PUBLISHED_RECORD_KIND_COMMENT || kind == null) && type == CommentSubjectTool.TYPE_VIDEO -> {
                                onOpenVideoDetail(
                                    VideoTarget.Ugc(
                                        aid = oid,
                                        cid = 0L,
                                        src = VideoTargetTool.default()
                                    )
                                )
                            }
                            else -> onOpenDynamicDetail(oid.toString())
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isDetailMode) {
            if (replyThread != null) {
                CommentThreadPane(
                    state = replyThread,
                    listState = threadListState,
                    currentMid = uiState.currentMid,
                    busyReplyIds = uiState.busyReplyIds,
                    onReplyAction = onReplyAction,
                    onDismiss = onDismissDetail,
                    onToggleSort = viewModel::toggleReplyThreadSort,
                    onLoadMore = viewModel::loadMoreReplyThread,
                    bottomPadding = COMMENT_FAB_SPACE,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = listContentPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (header != null) {
                    item(key = "panel_header", contentType = "panel_header") {
                        header()
                    }
                }

                item(
                    key = "comment_header",
                    contentType = "header"
                ) {
                    CommentHeader(
                        state = uiState,
                        onSelectSort = viewModel::selectSort
                    )
                }

                when {
                    subject == null -> {
                        item(
                            key = "comment_empty_subject",
                            contentType = "state"
                        ) {
                            StateMessageCard(text = "暂无评论信息")
                        }
                    }

                    isInitLoading -> {
                        items(
                            count = INIT_SKELETON_COUNT,
                            key = { index -> "comment_skeleton_$index" },
                            contentType = { "reply_skeleton" }
                        ) {
                            CommentCardSkeleton()
                        }
                    }

                    !uiState.error.isNullOrBlank() && uiState.items.isEmpty() -> {
                        item(
                            key = "comment_error",
                            contentType = "state"
                        ) {
                            StateMessageCard(text = uiState.error.orEmpty(), isError = true)
                        }
                    }

                    uiState.items.isEmpty() -> {
                        item(
                            key = "comment_no_data",
                            contentType = "state"
                        ) {
                            StateMessageCard(text = "还没有评论")
                        }
                    }

                    else -> {
                        items(
                            items = uiState.items,
                            key = { it.rpid },
                            contentType = { "reply" }
                        ) { reply ->
                            CommentCard(
                                reply = reply,
                                currentMid = uiState.currentMid,
                                busyReplyIds = uiState.busyReplyIds,
                                onAction = onReplyAction
                            )
                        }
                    }
                }

                if (uiState.loadingMore) {
                    items(
                        count = LOAD_MORE_SKELETON_COUNT,
                        key = { index -> "comment_loading_more_$index" },
                        contentType = { "reply_skeleton" }
                    ) {
                        CommentCardSkeleton()
                    }
                } else if (!uiState.loadMoreError.isNullOrBlank()) {
                    item(
                        key = "comment_load_more_error",
                        contentType = "footer"
                    ) {
                        StateMessageCard(text = uiState.loadMoreError.orEmpty(), isError = true)
                    }
                } else if (!uiState.hasMore && uiState.items.isNotEmpty()) {
                    item(
                        key = "comment_end",
                        contentType = "footer"
                    ) {
                        StateMessageCard(
                            text = uiState.endText
                                ?: if (uiState.sort == CommentSort.HOT) {
                                    "热门评论已展示完"
                                } else {
                                    "没有更多评论"
                                }
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = replyThread,
                contentKey = { it != null },
                transitionSpec = {
                    (slideInHorizontally { fullWidth -> fullWidth } + fadeIn())
                        .togetherWith(slideOutHorizontally { fullWidth -> fullWidth } + fadeOut())
                },
                label = "comment_thread_pane"
            ) { threadPane ->
                if (threadPane != null) {
                    CommentThreadPane(
                        state = threadPane,
                        listState = threadListState,
                        currentMid = uiState.currentMid,
                        busyReplyIds = uiState.busyReplyIds,
                        onReplyAction = onReplyAction,
                        onDismiss = viewModel::closeReplyThread,
                        onToggleSort = viewModel::toggleReplyThreadSort,
                        onLoadMore = viewModel::loadMoreReplyThread,
                        bottomPadding = COMMENT_FAB_SPACE,
                        modifier = Modifier.fillMaxSize(),
                        isDetailMode = false
                    )
                }
            }
        }
        CommentEditorLayer(
            hasSubject = uiState.subject != null,
            currentMid = uiState.currentMid,
            isReplyThreadOpen = replyThread != null,
            fabVisible = fabVisible,
            fabEndPadding = contentPadding.calculateEndPadding(layoutDirection),
            fabBottomPadding = contentPadding.calculateBottomPadding() + 16.dp,
            viewModel = viewModel
        )
    }
    uiState.replyCheckDialogText?.let { text ->
        AlertDialog(
            onDismissRequest = viewModel::dismissReplyCheckDialog,
            title = { Text("评论检查") },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissReplyCheckDialog) {
                    Text("确定")
                }
            }
        )
    }
}

@Composable
private fun BoxScope.CommentEditorLayer(
    hasSubject: Boolean,
    currentMid: Long,
    isReplyThreadOpen: Boolean,
    fabVisible: Boolean,
    fabEndPadding: androidx.compose.ui.unit.Dp,
    fabBottomPadding: androidx.compose.ui.unit.Dp,
    viewModel: CommentViewModel
) {
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()

    if (hasSubject) {
        AnimatedVisibility(
            visible = fabVisible,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = fabEndPadding,
                    bottom = fabBottomPadding
                ),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            CommentEditorFab(
                contentDescription = when {
                    currentMid <= 0L -> "登录后发评论"
                    isReplyThreadOpen -> "回复评论"
                    else -> "发表评论"
                },
                onClick = viewModel::openEditor
            )
        }
    }

    CommentEditorSheet(
        state = editorState,
        onDismiss = viewModel::dismissEditor,
        onValueChange = viewModel::updateEditorInput,
        onSubmit = { viewModel.submitEditor() },
        onAddImages = viewModel::addImages,
        onRemoveImage = viewModel::removeImage
    )
}

@Composable
private fun CommentHeader(
    state: CommentUiState,
    onSelectSort: (CommentSort) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = headerCount(state.count),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (state.canSwitchSort) {
            CommentSortSelector(
                sort = state.sort,
                onSelectSort = onSelectSort
            )
        } else {
            Text(
                text = sortText(state.sort),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommentSortSelector(
    sort: CommentSort,
    onSelectSort: (CommentSort) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentSort.entries.forEachIndexed { index, option ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.height(16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            val selected = option == sort
            Text(
                text = sortText(option),
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
                modifier = Modifier
                    .selectable(
                        selected = selected,
                        onClick = { onSelectSort(option) },
                        role = Role.RadioButton,
                        interactionSource = remember(option) { MutableInteractionSource() },
                        indication = null
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

private fun sortText(sort: CommentSort): String {
    return when (sort) {
        CommentSort.HOT -> "按热度"
        CommentSort.TIME -> "按时间"
    }
}

private fun headerCount(count: Long): String {
    return if (count > 0L) {
        "$count 条评论"
    } else {
        "暂无评论"
    }
}

private const val INIT_SKELETON_COUNT = 4
private const val LOAD_MORE_SKELETON_COUNT = 2
private val COMMENT_FAB_SPACE = 88.dp
private const val COMMENT_TAG = "CommentPanel"

private fun CommentUser.toSpaceRoute(subject: CommentSubject?): SpaceRoute? {
    if (mid <= 0L && name.isBlank()) return null
    return SpaceRoute(
        mid = mid,
        name = name.takeIf(String::isNotBlank),
        fromViewAid = subject
            ?.takeIf { it.type == CommentSubjectTool.TYPE_VIDEO }
            ?.oid
            ?.takeIf { it > 0L }
    )
}
