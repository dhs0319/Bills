package com.dhs0319.bills.feature.im

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.theme.LocalTopLevelNavSpace
import com.dhs0319.bills.core.model.ImSessionItem
import com.dhs0319.bills.core.model.ImSessionTab
import com.dhs0319.bills.feature.im.component.ImNotificationEntries
import com.dhs0319.bills.feature.im.component.ImSessionDropdown
import com.dhs0319.bills.feature.im.component.ImSessionSwipeItem
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImScreen(
    onOpenConversation: (ImSessionItem) -> Unit,
    onOpenMsgFeed: () -> Unit = {},
    refreshRequest: Int = 0,
    vm: ImViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val topLevelNavSpace = LocalTopLevelNavSpace.current
    val listStates = ImSessionTab.entries.associateWith { tab ->
        key(tab) { rememberLazyListState() }
    }
    val listState = listStates.getValue(state.currentTab)
    var openedSessionKey by remember { mutableStateOf<String?>(null) }
    var pendingDelete by remember { mutableStateOf<ImSessionItem?>(null) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling -> if (scrolling) openedSessionKey = null }
    }

    LaunchedEffect(refreshRequest) {
        if (refreshRequest > 0) {
            if (state.sessions.isNotEmpty()) {
                listState.scrollToItem(0)
            }
            vm.refresh()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("消息") },
                actions = {
                    ImSessionDropdown(
                        tabs = state.tabs,
                        selectedTab = state.currentTab,
                        onSelect = {
                            openedSessionKey = null
                            vm.selectTab(it)
                        },
                        modifier = Modifier.padding(end = 4.dp),
                        enabled = state.isLoggedIn &&
                            !state.isLoading && !state.isRefreshing && !state.isLoadingMore
                    )
                }
            )
        }
    ) { padding ->
        BiliPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!state.isLoggedIn) {
                StateMessageCard(
                    text = "请先登录后查看消息",
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                )
                return@BiliPullToRefreshBox
            }

            Column(modifier = Modifier.fillMaxSize()) {
                LaunchedEffect(
                    state.currentTab,
                    listState,
                    state.sessions.size,
                    state.canLoadMore,
                    state.isLoadingMore,
                    state.loadMoreError
                ) {
                    snapshotFlow {
                        val total = listState.layoutInfo.totalItemsCount
                        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                        state.sessions.isNotEmpty() &&
                            state.canLoadMore &&
                            state.loadMoreError.isNullOrBlank() &&
                            total > 0 &&
                            last >= total - 4
                    }
                        .distinctUntilChanged()
                        .collect { shouldLoadMore ->
                            if (shouldLoadMore) vm.loadMore()
                        }
                }

                // Dispose the previous category's item animations when switching lists.
                key(state.currentTab) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 12.dp,
                            top = 8.dp,
                            end = 12.dp,
                            bottom = topLevelNavSpace + 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(key = "im_notification_entries") {
                            ImNotificationEntries(onOpenReplyAndMentions = onOpenMsgFeed)
                        }

                        if (!state.actionError.isNullOrBlank()) {
                            item(key = "im_action_error") {
                                StateMessageCard(
                                    text = state.actionError.orEmpty(),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    isError = true,
                                    actionText = "知道了",
                                    onAction = vm::clearActionError
                                )
                            }
                        }

                        if (state.sessions.isEmpty()) {
                            item(key = "im_empty_state") {
                                when {
                                    state.isLoading -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator()
                                        }
                                    }

                                    !state.errorMessage.isNullOrBlank() -> {
                                        StateMessageCard(
                                            text = state.errorMessage.orEmpty(),
                                            modifier = Modifier.padding(vertical = 16.dp),
                                            isError = true
                                        )
                                    }

                                    else -> {
                                        StateMessageCard(
                                            text = "暂无消息",
                                            modifier = Modifier.padding(vertical = 16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            items = state.sessions,
                            key = { it.key }
                        ) { item ->
                            ImSessionSwipeItem(
                                item = item,
                                modifier = Modifier.animateItem(placementSpec = null),
                                isOpen = openedSessionKey == item.key,
                                onSwipeStateChange = { open ->
                                    openedSessionKey = if (open) {
                                        item.key
                                    } else {
                                        openedSessionKey.takeIf { it != item.key }
                                    }
                                },
                                onTogglePin = { vm.togglePin(item) },
                                onDelete = { pendingDelete = item },
                                onClick = if (item.talkerId != null && item.sessionType != null) {
                                    { onOpenConversation(item) }
                                } else {
                                    null
                                }
                            )
                        }

                        if (state.sessions.isNotEmpty() && !state.errorMessage.isNullOrBlank()) {
                            item(key = "im_error_footer") {
                                StateMessageCard(
                                    text = state.errorMessage.orEmpty(),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    isError = true
                                )
                            }
                        }

                        if (state.isLoadingMore) {
                            item(key = "im_loading_more") {
                                ImLoadingMore()
                            }
                        } else if (!state.loadMoreError.isNullOrBlank()) {
                            item(key = "im_load_more_error") {
                                StateMessageCard(
                                    text = state.loadMoreError.orEmpty(),
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    isError = true,
                                    actionText = "重试",
                                    onAction = vm::loadMore
                                )
                            }
                        } else if (
                            state.sessions.isNotEmpty() &&
                            state.paginationParams?.hasMore != true &&
                            !state.isLoading &&
                            !state.isRefreshing &&
                            state.errorMessage.isNullOrBlank()
                        ) {
                            item(key = "im_no_more") {
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

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除会话") },
            text = {
                Text("确定删除与" + "「" + target.name + "」" + "的会话吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        vm.deleteSession(target)
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun ImLoadingMore(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

