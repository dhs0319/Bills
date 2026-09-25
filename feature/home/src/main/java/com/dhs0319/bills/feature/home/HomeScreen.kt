package com.dhs0319.bills.feature.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.CollapsingTopBarScaffold
import com.dhs0319.bills.core.designsystem.component.PagerSlidingTabRow
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.listen.ListenItem
import com.dhs0319.bills.feature.home.article.HomeArticlePage
import com.dhs0319.bills.feature.home.interest.InterestDialog
import com.dhs0319.bills.feature.home.listen.ListenHomePage
import com.dhs0319.bills.feature.home.live.HomeLivePage
import com.dhs0319.bills.feature.home.video.HomeVideoPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val homeTabs = listOf("FM", "推荐", "直播", "专栏")
private val homeProfileAvatarSize = 38.dp
private val homeSearchRowHeight = 48.dp
private val homeTopContentPadding = 4.dp
private const val homeDefaultPage = 1
private const val homeScrollToTopInstantThresholdViewports = 8f

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSearch: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    profileAvatar: String? = null,
    showTopActions: Boolean = true,
    onOpenVideo: (VideoTarget) -> Unit = {},
    onOpenSpace: (SpaceRoute) -> Unit = {},
    onOpenLive: (LiveRoute) -> Unit = {},
    onOpenDynamic: (String) -> Unit = {},
    onOpenArticle: (String, Int) -> Unit = { _, _ -> },
    onOpenListenItem: (Long, Int, Long, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    refreshRequest: Int = 0,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val pagerState = rememberPagerState(initialPage = homeDefaultPage, pageCount = { homeTabs.size })
    val listenGridState = rememberLazyStaggeredGridState()
    val videoGridState = rememberLazyStaggeredGridState()
    val liveGridState = rememberLazyStaggeredGridState()
    val articleGridState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    var scrollToTopJob by remember { mutableStateOf<Job?>(null) }
    var listenRefreshRequest by rememberSaveable { mutableIntStateOf(0) }
    var videoRefreshRequest by rememberSaveable { mutableIntStateOf(0) }
    var liveRefreshRequest by rememberSaveable { mutableIntStateOf(0) }
    var articleRefreshRequest by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        viewModel.refreshPageAction()
    }
    var handledRefreshRequest by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(refreshRequest) {
        if (refreshRequest == 0) {
            handledRefreshRequest = 0
        } else if (refreshRequest != handledRefreshRequest) {
            handledRefreshRequest = refreshRequest
            when (pagerState.currentPage) {
                0 -> listenRefreshRequest++
                1 -> videoRefreshRequest++
                2 -> liveRefreshRequest++
                else -> articleRefreshRequest++
            }
        }
    }
    state.interestChoose?.let { interestChoose ->
        InterestDialog(
            data = interestChoose,
            onDismiss = viewModel::dismissInterest,
            onConfirm = { id, result, posIds -> viewModel.submitInterest(id, result, posIds) }
        )
    }

    CollapsingTopBarScaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { scrollBehavior ->
            LaunchedEffect(refreshRequest) {
                if (refreshRequest > 0) {
                    scrollBehavior.state.heightOffset = 0f
                    scrollBehavior.state.contentOffset = 0f
                }
            }
            HomeTopBar(
                scrollBehavior = scrollBehavior,
                pagerState = pagerState,
                onNavigateToSearch = onNavigateToSearch,
                onNavigateToProfile = onNavigateToProfile,
                profileAvatar = profileAvatar,
                showTopActions = showTopActions,
                onReselectTab = { page ->
                    val gridState = when (page) {
                        0 -> listenGridState
                        1 -> videoGridState
                        2 -> liveGridState
                        else -> articleGridState
                    }
                    scrollToTopJob?.cancel()
                    scrollToTopJob = scope.launch {
                        gridState.animateToTop()
                    }
                }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> ListenHomePage(
                    isActive = pagerState.currentPage == page,
                    refreshRequest = listenRefreshRequest,
                    gridState = listenGridState,
                    onItemClick = { item ->
                        onOpenListenItem(
                            item.oid,
                            item.itemType,
                            item.subId,
                            item.title,
                            item.author,
                            item.cover
                        )
                    }
                )

                1 -> HomeVideoPage(
                    paging = state.paging,
                    isActive = pagerState.currentPage == page,
                    onActivate = viewModel::activate,
                    onRetryLoadMore = viewModel::retryLoadMore,
                    toastMessage = state.toastMessage,
                    dislikedReasons = state.dislikedReasons,
                    refreshRequest = videoRefreshRequest,
                    gridState = videoGridState,
                    onRefresh = viewModel::refresh,
                    onLoadMore = viewModel::loadMore,
                    onOpenVideo = onOpenVideo,
                    onOpenSpace = onOpenSpace,
                    onOpenLive = onOpenLive,
                    onOpenDynamic = onOpenDynamic,
                    onDislike = viewModel::submitDislike,
                    onCancelDislike = viewModel::cancelDislike,
                    onToastShown = viewModel::consumeToast
                )

                2 -> HomeLivePage(
                    isActive = pagerState.currentPage == page,
                    refreshRequest = liveRefreshRequest,
                    gridState = liveGridState,
                    onOpenLive = onOpenLive,
                    onOpenSpace = onOpenSpace
                )

                else -> HomeArticlePage(
                    isActive = pagerState.currentPage == page,
                    refreshRequest = articleRefreshRequest,
                    gridState = articleGridState,
                    onOpenArticle = onOpenArticle,
                    onOpenSpace = onOpenSpace
                )
            }
        }
    }
}

private suspend fun LazyStaggeredGridState.animateToTop() {
    if (!canScrollBackward) return

    scroll {
        val lazyScope = LazyLayoutScrollScope(this@animateToTop, this)
        val viewportPx = layoutInfo.viewportSize.height.coerceAtLeast(1).toFloat()
        val remainingPx = abs(lazyScope.calculateDistanceTo(0).toFloat())
        if (remainingPx >= viewportPx * homeScrollToTopInstantThresholdViewports) {
            lazyScope.snapToItem(0)
            return@scroll
        }
        val durationSeconds = (180f + remainingPx / viewportPx * 35f)
            .coerceAtMost(700f) / 1000f
        val pixelsPerSecond = (remainingPx / durationSeconds)
            .coerceIn(80f, (viewportPx * 18f).coerceAtLeast(80f))
        var previousFrame = withFrameNanos { it }

        while (this@animateToTop.canScrollBackward) {
            val frame = withFrameNanos { it }
            val elapsedNanos = (frame - previousFrame).coerceIn(0L, 32_000_000L)
            if (elapsedNanos == 0L) continue
            val consumed = lazyScope.scrollBy(-pixelsPerSecond * elapsedNanos / 1_000_000_000f)
            if (consumed == 0f) break
            previousFrame = frame
        }
        if (this@animateToTop.canScrollBackward &&
            abs(lazyScope.calculateDistanceTo(0)) <= 2
        ) {
            lazyScope.snapToItem(0)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    pagerState: PagerState,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    profileAvatar: String?,
    showTopActions: Boolean,
    onReselectTab: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val state = scrollBehavior.state
                val limit = if (showTopActions) {
                    -(homeSearchRowHeight.roundToPx() + homeTopContentPadding.roundToPx()).toFloat()
                } else {
                    0f
                }
                if (state.heightOffsetLimit != limit) state.heightOffsetLimit = limit
                val offsetY = state.heightOffset.roundToInt()
                val layoutHeight = (placeable.height + offsetY).coerceAtLeast(0)
                layout(placeable.width, layoutHeight) {
                    placeable.placeRelative(0, offsetY)
                }
            }
            .padding(top = homeTopContentPadding)
    ) {
        if (showTopActions) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .weight(1f)
                        .height(homeSearchRowHeight),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "搜索",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onNavigateToProfile) {
                    AvatarImage(
                        url = profileAvatar,
                        contentDescription = "我的",
                        modifier = Modifier.size(homeProfileAvatarSize)
                    )
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            PagerSlidingTabRow(
                tabs = homeTabs,
                pagerState = pagerState,
                modifier = Modifier.widthIn(max = 288.dp),
                onReselect = onReselectTab
            )
        }
    }
}
