package com.dhs0319.bills.feature.im.followers

import android.text.format.DateFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.im.NewFollowerItem
import java.util.Calendar
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun NewFollowersScreen(
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    vm: NewFollowersViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.followError) {
        state.followError?.let {
            snackbarHostState.showSnackbar(it)
            vm.dismissFollowError()
        }
    }

    NewFollowersContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onOpenSpace = onOpenSpace,
        onRefresh = vm::refresh,
        onLoadMore = vm::loadMore,
        onFollowClick = vm::onFollowClick,
        onConfirmUnfollow = vm::confirmUnfollow,
        onDismissUnfollow = vm::dismissUnfollow
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewFollowersContent(
    state: NewFollowersUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onFollowClick: (Long) -> Unit,
    onConfirmUnfollow: () -> Unit,
    onDismissUnfollow: () -> Unit
) {
    val listState = rememberLazyListState()
    val layoutDirection = LocalLayoutDirection.current

    LaunchedEffect(
        listState, state.page, state.hasMore, state.isBusy,
        state.followingMids, state.pendingUnfollowMid, state.errorMessage, state.loadMoreError
    ) {
        snapshotFlow {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.hasMore && !state.isBusy && state.followingMids.isEmpty() &&
                state.pendingUnfollowMid == null &&
                state.errorMessage == null && state.loadMoreError == null &&
                total > 0 && last >= total - 4
        }.distinctUntilChanged().collect { if (it) onLoadMore() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("新增粉丝") },
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
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        top = 12.dp,
                        end = 12.dp,
                        bottom = padding.calculateBottomPadding() + 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!state.isLoggedIn) {
                        item(key = "login") {
                            StateMessageCard(
                                text = "请先登录后查看新增粉丝"
                            )
                        }
                    } else {
                        if (state.isLoading) {
                            item(key = "loading") { FollowersLoading() }
                        }
                        state.errorMessage?.let { message ->
                            item(key = "error") {
                                StateMessageCard(
                                    text = message,
                                    isError = true,
                                    actionText = "重试",
                                    onAction = onRefresh
                                )
                            }
                        }
                        if (state.items.isEmpty() && !state.isBusy && state.errorMessage == null) {
                            item(key = "empty") {
                                StateMessageCard(text = "暂无新增粉丝")
                            }
                        }
                        items(state.items, key = { it.mid }) { follower ->
                            NewFollowerCard(
                                follower = follower,
                                isUpdatingRelation = follower.mid in state.followingMids,
                                canFollow = !state.isBusy,
                                onOpenSpace = { onOpenSpace(SpaceRoute(mid = follower.mid, name = follower.name)) },
                                onFollowClick = { onFollowClick(follower.mid) }
                            )
                        }
                        when {
                            state.isLoadingMore -> item(key = "loading_more") { FollowersLoading() }
                            state.loadMoreError != null -> item(key = "load_more_error") {
                                StateMessageCard(
                                    text = state.loadMoreError,
                                    isError = true,
                                    actionText = "重试",
                                    onAction = onLoadMore
                                )
                            }
                            state.items.isNotEmpty() && !state.hasMore && !state.isBusy &&
                                state.errorMessage == null -> item(key = "end") {
                                Text(
                                    text = "没有更多啦~",
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
    state.items.firstOrNull { it.mid == state.pendingUnfollowMid && it.isFollowing }?.let { follower ->
        AlertDialog(
            onDismissRequest = onDismissUnfollow,
            title = { Text("取消关注") },
            text = { Text("确定要取消关注「${follower.name}」吗？") },
            dismissButton = {
                TextButton(onClick = onDismissUnfollow) { Text("取消") }
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmUnfollow,
                    enabled = !state.isBusy && follower.mid !in state.followingMids
                ) {
                    Text("取消关注")
                }
            }
        )
    }
}

@Composable
private fun NewFollowerCard(
    follower: NewFollowerItem,
    isUpdatingRelation: Boolean,
    canFollow: Boolean,
    onOpenSpace: () -> Unit,
    onFollowClick: () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    Card(onClick = onOpenSpace, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(
                url = follower.avatar,
                contentDescription = "${follower.name}的头像",
                modifier = Modifier.size(40.dp)
            )
            BoxWithConstraints(
                modifier = Modifier.weight(1f)
            ) {
                val stackFollowMessage = fontScale >= 1.5f || maxWidth < 140.dp * fontScale
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (stackFollowMessage) {
                        Text(
                            text = follower.name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "关注了我",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = follower.name,
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "关注了我",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                    if (follower.followedAt > 0L) {
                        Text(
                            text = followerDate(follower.followedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val buttonContainerColor = if (follower.isFollowing) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.primary
            }
            val buttonContentColor = if (follower.isFollowing) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onPrimary
            }
            Button(
                onClick = onFollowClick,
                enabled = canFollow && !isUpdatingRelation,
                modifier = Modifier.heightIn(min = 32.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                border = if (follower.isFollowing) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                } else {
                    null
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonContainerColor,
                    contentColor = buttonContentColor,
                    disabledContainerColor = buttonContainerColor,
                    disabledContentColor = buttonContentColor
                )
            ) {
                if (isUpdatingRelation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = buttonContentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (follower.isFollowing) "已互粉" else "回关",
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowersLoading() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

private fun followerDate(timestamp: Long): String {
    val date = Calendar.getInstance().apply { timeInMillis = timestamp * 1000L }
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val pattern = if (date.get(Calendar.YEAR) == currentYear) "M月d日" else "yyyy年M月d日"
    return DateFormat.format(pattern, date).toString()
}
