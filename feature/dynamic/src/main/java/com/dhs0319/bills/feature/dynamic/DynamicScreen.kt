package com.dhs0319.bills.feature.dynamic

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.BiliPullToRefreshBox
import com.dhs0319.bills.core.designsystem.component.LoginRequiredState
import com.dhs0319.bills.core.designsystem.component.PageState
import com.dhs0319.bills.core.designsystem.theme.LocalTopLevelNavSpace
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.dynamic.feed.DynamicFeed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicScreen(
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenLive: (LiveRoute) -> Unit,
    onOpenDynamic: (String) -> Unit,
    onLogin: () -> Unit,
    refreshRequest: Int = 0,
    viewModel: DynamicViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val topLevelNavSpace = LocalTopLevelNavSpace.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshVisitedUps()
    }
    val shouldLoadMore by remember(
        state.items.size,
        state.canLoadMore,
        state.isLoadingMore,
        state.loadMoreError,
        listState
    ) {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            state.canLoadMore &&
                state.loadMoreError.isNullOrBlank() &&
                total > 0 &&
                last >= total - LOAD_MORE_TRIGGER_OFFSET
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }
    LaunchedEffect(refreshRequest) {
        if (refreshRequest > 0) {
            if (state.items.isNotEmpty()) {
                listState.scrollToItem(0)
            }
            viewModel.refresh()
        }
    }

    LaunchedEffect(actionMessage) {
        actionMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.consumeActionMessage()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text("动态") }
            )
        }
    ) { padding ->
        if (!state.isLoggedIn) {
            LoginRequiredState(
                description = "登录后，查看你关注的 UP 主动态",
                onLogin = onLogin,
                modifier = Modifier.padding(padding).padding(bottom = topLevelNavSpace)
            )
            return@Scaffold
        }
        BiliPullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                state.errorMessage != null && state.items.isEmpty() -> {
                    PageState(
                        icon = Icons.Outlined.CloudOff,
                        title = "加载失败",
                        description = state.errorMessage.orEmpty().ifBlank { "加载动态失败" },
                        actionText = "重新加载",
                        onAction = viewModel::refresh,
                        modifier = Modifier.padding(bottom = topLevelNavSpace)
                    )
                }

                state.items.isEmpty() -> {
                    PageState(
                        icon = Icons.Outlined.DynamicFeed,
                        title = "暂无动态",
                        description = "关注 UP 主后，新的动态会出现在这里",
                        modifier = Modifier.padding(bottom = topLevelNavSpace)
                    )
                }

                else -> {
                    DynamicFeed(
                        currentMid = state.currentMid,
                        upList = state.upList,
                        items = state.items,
                        listState = listState,
                        isLoadingMore = state.isLoadingMore,
                        errorMessage = state.errorMessage,
                        loadMoreError = state.loadMoreError,
                        onRetryRefresh = viewModel::refresh,
                        onRetryLoadMore = viewModel::loadMore,
                        onOpenVideo = onOpenVideo,
                        onOpenSpace = onOpenSpace,
                        onVisitUp = viewModel::visitUp,
                        onOpenLive = onOpenLive,
                        onOpenDynamic = onOpenDynamic,
                        onAddToWatchLater = viewModel::addToWatchLater,
                        onUnfollow = viewModel::unfollow,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private const val LOAD_MORE_TRIGGER_OFFSET = 4
