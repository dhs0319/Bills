package com.dhs0319.bills.feature.im.likes

import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.CoverImage
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.PublishedRecord
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.WebLinkTarget
import com.dhs0319.bills.core.model.im.ReceivedLikeItem
import com.dhs0319.bills.core.model.im.ReceivedLikeType
import com.dhs0319.bills.feature.comment.CommentPanel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun ReceivedLikesScreen(
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenVideoDetail: (VideoTarget) -> Unit,
    onOpenDynamicDetail: (String) -> Unit,
    onOpenLiveDetail: (LiveRoute) -> Unit,
    vm: ReceivedLikesViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var detailRecord by remember { mutableStateOf<PublishedRecord?>(null) }

    BackHandler(enabled = detailRecord != null) { detailRecord = null }

    Box(modifier = Modifier.fillMaxSize()) {
        ReceivedLikesContent(
            state = state,
            onBack = onBack,
            onOpenSpace = onOpenSpace,
            onRefresh = vm::refresh,
            onLoadMore = vm::loadMore,
            onOpenItem = { item ->
                when (val destination = item.destination()) {
                    is ReceivedLikeDestination.Comment -> detailRecord = destination.record
                    is ReceivedLikeDestination.Dynamic -> onOpenDynamicDetail(destination.id)
                    is ReceivedLikeDestination.Content -> when (val target = destination.target) {
                        is WebLinkTarget.ToVideo -> onOpenVideoDetail(target.target)
                        is WebLinkTarget.ToSpace -> onOpenSpace(SpaceRoute(mid = target.mid))
                        is WebLinkTarget.ToLive -> onOpenLiveDetail(LiveRoute(roomId = target.roomId))
                        WebLinkTarget.External, WebLinkTarget.Stay -> Unit
                    }
                    null -> Toast.makeText(context, "暂时无法打开该消息对应的内容", Toast.LENGTH_SHORT).show()
                }
            }
        )
        AnimatedVisibility(
            visible = detailRecord != null,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            CommentPanel(
                subject = null,
                detailRecord = detailRecord,
                onDismissDetail = { detailRecord = null },
                onOpenSpace = onOpenSpace,
                onOpenVideoDetail = onOpenVideoDetail,
                onOpenDynamicDetail = onOpenDynamicDetail,
                onOpenLiveDetail = onOpenLiveDetail,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceivedLikesContent(
    state: ReceivedLikesUiState,
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenItem: (ReceivedLikeItem) -> Unit
) {
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(listState, state.cursor, state.isBusy, state.errorMessage, state.loadMoreError) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.canLoadMore && !state.isBusy &&
                state.errorMessage == null && state.loadMoreError == null &&
                total > 0 && last >= total - 4
        }.distinctUntilChanged().collect { if (it) onLoadMore() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收到喜欢") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    top = padding.calculateTopPadding(),
                    end = padding.calculateEndPadding(layoutDirection)
                )
                .consumeWindowInsets(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            BiliPullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.widthIn(max = 600.dp).fillMaxSize()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Draw behind the gesture bar; keep the final item clear of it when scrolled to the end.
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp,
                        bottom = 12.dp + padding.calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!state.isLoggedIn) {
                        item(key = "login") {
                            StateMessageCard(text = "请先登录后查看收到的喜欢")
                        }
                    } else {
                        if (state.isLoading || (state.items.isEmpty() && state.canLoadMore &&
                                !state.isLoadingMore && state.errorMessage == null && state.loadMoreError == null)
                        ) {
                            item(key = "loading") { ReceivedLikesLoading() }
                        }
                        if (state.errorMessage != null) {
                            item(key = "error") {
                                StateMessageCard(
                                    text = state.errorMessage.orEmpty(),
                                    isError = true,
                                    actionText = "重试",
                                    onAction = onRefresh
                                )
                            }
                        }
                        if (state.items.isEmpty() && !state.isBusy && !state.canLoadMore &&
                            state.errorMessage == null && state.loadMoreError == null
                        ) {
                            item(key = "empty") {
                                StateMessageCard(text = state.notice ?: "还没有收到喜欢")
                            }
                        }
                        items(state.items, key = { it.key }) { item ->
                            ReceivedLikeCard(
                                item = item,
                                onOpenSpace = onOpenSpace,
                                onClick = { onOpenItem(item) }
                            )
                        }
                        when {
                            state.isLoadingMore -> {
                                item(key = "loading_more") { ReceivedLikesLoading() }
                            }
                            state.loadMoreError != null -> {
                                item(key = "load_more_error") {
                                    StateMessageCard(
                                        text = state.loadMoreError.orEmpty(),
                                        isError = true,
                                        actionText = "重试",
                                        onAction = onLoadMore
                                    )
                                }
                            }
                            state.items.isNotEmpty() && !state.canLoadMore && !state.isBusy &&
                                state.errorMessage == null -> {
                                item(key = "end") {
                                    Text(
                                        text = state.notice ?: "没有更多啦~",
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceivedLikesLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ReceivedLikeCard(
    item: ReceivedLikeItem,
    onOpenSpace: (SpaceRoute) -> Unit,
    onClick: () -> Unit
) {
    val user = item.users.firstOrNull()
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (user != null) {
                    AvatarImage(
                        url = user.avatar,
                        contentDescription = user.name.ifBlank { "用户头像" },
                        modifier = Modifier.size(40.dp).clickable(enabled = user.mid > 0L) {
                            onOpenSpace(SpaceRoute(mid = user.mid, name = user.name))
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = item.description.ifBlank { item.fallbackDescription() },
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (item.msgTime > 0L) {
                        Text(
                            text = DateFormat.format("yyyy-MM-dd HH:mm", item.msgTime * 1000L).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (item.coverText.isNotBlank() || item.coverImage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.coverText.isNotBlank()) {
                            Text(
                                text = item.coverText,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.coverImage != null) {
                            CoverImage(
                                url = item.coverImage,
                                contentDescription = "相关内容封面",
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ReceivedLikeItem.fallbackDescription(): String {
    val names = users.take(2).joinToString("、") { it.name.ifBlank { "用户" } }.ifBlank { "有人" }
    val total = maxOf(count, users.size.toLong())
    val actors = if (total > 1L) "${names}等${total}人" else names
    val action = when (type) {
        ReceivedLikeType.LIKE -> "赞了你的内容"
        ReceivedLikeType.COIN -> "给你的内容投了币"
        ReceivedLikeType.FAVORITE -> "收藏了你的内容"
    }
    return actors + action
}
