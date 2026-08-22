package com.dhs0319.bills.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dhs0319.bills.core.designsystem.component.roundScreenSafePadding
import com.dhs0319.bills.core.designsystem.theme.ThemeConfig
import com.dhs0319.bills.core.designsystem.theme.buildNavTransitions
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.core.model.HistoryTarget
import com.dhs0319.bills.core.model.ImSessionItem
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.StreamPlaybackTarget
import androidx.compose.runtime.CompositionLocalProvider
import com.dhs0319.bills.core.model.VideoSrc
import com.dhs0319.bills.core.model.VideoTarget

import com.dhs0319.bills.core.model.VideoTargetTool
import com.dhs0319.bills.core.model.WebLinkTarget
import com.dhs0319.bills.feature.dynamic.DynamicScreen
import com.dhs0319.bills.feature.dynamic.navigation.dynamicDetailScreen
import com.dhs0319.bills.feature.dynamic.navigation.navigateToDynamicDetail
import com.dhs0319.bills.feature.auth.navigation.ACCOUNT_ROUTE
import com.dhs0319.bills.feature.auth.navigation.LOGIN_ROUTE
import com.dhs0319.bills.feature.auth.navigation.SMS_LOGIN_ROUTE
import com.dhs0319.bills.feature.auth.navigation.accountScreen
import com.dhs0319.bills.feature.auth.navigation.loginScreen
import com.dhs0319.bills.feature.auth.navigation.smsLoginScreen
import com.dhs0319.bills.feature.download.navigation.downloadScreen
import com.dhs0319.bills.feature.download.navigation.navigateToDownload
import com.dhs0319.bills.feature.favorite.navigation.favoriteScreen
import com.dhs0319.bills.feature.favorite.navigation.navigateToFavorite
import com.dhs0319.bills.feature.favorite.navigation.navigateToFavoriteFolder
import com.dhs0319.bills.feature.history.navigation.historyScreen
import com.dhs0319.bills.feature.history.navigation.historySearchScreen
import com.dhs0319.bills.feature.history.navigation.navigateToHistory
import com.dhs0319.bills.feature.history.navigation.navigateToHistorySearch
import com.dhs0319.bills.feature.history.navigation.navigateToWatchLater
import com.dhs0319.bills.feature.history.navigation.watchLaterScreen
import com.dhs0319.bills.feature.home.HomeScreen
import com.dhs0319.bills.feature.im.ImScreen
import com.dhs0319.bills.feature.im.navigation.imConversationScreen
import com.dhs0319.bills.feature.im.navigation.imMsgFeedScreen
import com.dhs0319.bills.feature.im.navigation.navigateToImConversation
import com.dhs0319.bills.feature.im.navigation.navigateToMsgFeed
import com.dhs0319.bills.feature.listen.navigation.listenDetailScreen
import com.dhs0319.bills.feature.listen.navigation.navigateToListenDetail
import com.dhs0319.bills.feature.live.LiveViewModel
import com.dhs0319.bills.feature.search.navigation.navigateToSearch
import com.dhs0319.bills.feature.search.navigation.searchScreen
import com.dhs0319.bills.feature.space.navigation.navigateToSpace
import com.dhs0319.bills.feature.space.navigation.spaceScreen
import com.dhs0319.bills.feature.space.navigation.spaceRelationScreen
import com.dhs0319.bills.feature.space.navigation.navigateToSpaceRelation
import com.dhs0319.bills.feature.home.interest.InterestScreen
import com.dhs0319.bills.feature.settings.SettingsViewModel
import com.dhs0319.bills.feature.settings.navigation.HOME_INTEREST_ROUTE
import com.dhs0319.bills.feature.settings.navigation.SETTINGS_ROUTE
import com.dhs0319.bills.feature.settings.navigation.settingsScreen
import com.dhs0319.bills.feature.download.DownloadViewModel
import com.dhs0319.bills.feature.user.UserScreen
import com.dhs0319.bills.feature.user.UserDest
import com.dhs0319.bills.feature.user.UserViewModel
import com.dhs0319.bills.feature.video.VideoViewModel
import com.dhs0319.bills.playback.PlaybackHost
import com.dhs0319.bills.playback.PlaybackHostViewModel

private const val MAIN_ROUTE = "main"
private val topLevelNavEdgePadding = 16.dp
private val topLevelNavGap = 8.dp
private val topLevelNavScrollThreshold = 72.dp
private const val topLevelNavAnimationDurationMillis = 220
@Composable
fun AppNavHost(
    themeConfig: ThemeConfig = ThemeConfig(),
    appLink: WebLinkTarget? = null,
    onAppLinkConsumed: () -> Unit = {}
) {
    val rootNavController = rememberNavController()
    val roundSafePadding = roundScreenSafePadding(themeConfig.roundScreenSafePaddingScale)
    var currentTab by rememberSaveable { mutableStateOf(TopLevelRoute.HOME) }
    val downloadViewModel: DownloadViewModel = hiltViewModel()
    val playbackHostViewModel: PlaybackHostViewModel = hiltViewModel()
    val videoViewModel: VideoViewModel = hiltViewModel()
    val liveViewModel: LiveViewModel = hiltViewModel()
    val hostMode = playbackHostViewModel.hostMode
    val closeVideoHost: () -> Unit = {
        playbackHostViewModel.close()
    }
    val dismissPlaybackHost: () -> Unit = {
        if (playbackHostViewModel.miniPlayerAvailable.value) {
            playbackHostViewModel.minimize()
        } else {
            when (playbackHostViewModel.currentTarget.value) {
                is StreamPlaybackTarget.Video -> closeVideoHost()
                is StreamPlaybackTarget.Live, null -> playbackHostViewModel.close()
            }
        }
    }
    val goHomeFromVideo: () -> Unit = {
        dismissPlaybackHost()
        rootNavController.popBackStack(MAIN_ROUTE, false)
        currentTab = TopLevelRoute.HOME
    }
    val openVideo: (VideoTarget) -> Unit = { target ->
        playbackHostViewModel.hide()
        rootNavController.navigateToVideo(target)
    }
    val openLive: (LiveRoute) -> Unit = { route ->
        liveViewModel.openRoute(route)
        playbackHostViewModel.openLive(route)
        playbackHostViewModel.expand()
    }
    val openHistoryTarget: (HistoryTarget?) -> Unit = { target ->
        when (target) {
            is HistoryTarget.Video -> openVideo(target.target)
            is HistoryTarget.Live -> openLive(target.route)
            is HistoryTarget.Article -> rootNavController.navigateToDynamicDetail(target.opusId,1)
            null -> Unit
        }
    }
    val openListenDetail: (Long, Int, Long, String, String, String) -> Unit = {
        oid,
        itemType,
        subId,
        title,
        author,
        cover ->
        playbackHostViewModel.close()
        rootNavController.navigateToListenDetail(oid, itemType, subId, title, author, cover)
    }
    val openArticle: (String, Int) -> Unit = { opusId, opusType ->
        playbackHostViewModel.minimize()
        rootNavController.navigateToDynamicDetail(opusId, opusType)
    }
    val transitions = remember(themeConfig.transitionStyle, themeConfig.animationSpeed) {
        buildNavTransitions<NavBackStackEntry>(
            themeConfig.transitionStyle,
            themeConfig.animationSpeed
        )
    }

    LaunchedEffect(appLink) {
        when (val target = appLink ?: return@LaunchedEffect) {
            is WebLinkTarget.ToVideo -> openVideo(target.target)
            is WebLinkTarget.ToSpace -> rootNavController.navigateToSpace(
                SpaceRoute(mid = target.mid)
            )
            is WebLinkTarget.ToLive -> openLive(
                LiveRoute(roomId = target.roomId)
            )
            is WebLinkTarget.External,
            is WebLinkTarget.Stay -> Unit
        }
        onAppLinkConsumed()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            modifier = Modifier
                .fillMaxSize()
                .padding(roundSafePadding),
            navController = rootNavController,
            startDestination = MAIN_ROUTE,
            enterTransition = { transitions.enter(this) },
            exitTransition = { transitions.exit(this) },
            popEnterTransition = { transitions.popEnter(this) },
            popExitTransition = { transitions.popExit(this) }
        ) {
            composable(MAIN_ROUTE) {
                MainTabsScaffold(
                    currentTab = currentTab,
                    onTabChange = { currentTab = it },
                    onNavigateToSearch = { rootNavController.navigateToSearch() },
                    onNavigateToSettings = { rootNavController.navigate(SETTINGS_ROUTE) },
                    onNavigateToAccount = { rootNavController.navigate(ACCOUNT_ROUTE) },
                    onNavigateFromUser = { dest ->
                        when (dest) {
                            UserDest.History -> rootNavController.navigateToHistory()
                            UserDest.Favorite -> rootNavController.navigateToFavorite()
                            UserDest.WatchLater -> rootNavController.navigateToWatchLater()
                        }
                    },
                    onNavigateToDownload = { rootNavController.navigateToDownload() },
                    onNavigateToVideo = openVideo,
                    onNavigateToSpace = rootNavController::navigateToSpace,
                    onNavigateToLive = openLive,
                    onNavigateToArticle = openArticle,
                    onNavigateToDynamicDetail = rootNavController::navigateToDynamicDetail,
                    onNavigateToListenDetail = openListenDetail,
                    onNavigateToMsgFeed = { rootNavController.navigateToMsgFeed() },
                    onNavigateToImConversation = { item ->
                        rootNavController.navigateToImConversation(item)
                    }
                )
            }

            videoScreen(
                videoViewModel = videoViewModel,
                playbackHostViewModel = playbackHostViewModel,
                onBack = {
                    if (rootNavController.popBackStack()) {
                        dismissPlaybackHost()
                    }
                },
                onGoHome = goHomeFromVideo,
                onOpenSpace = rootNavController::navigateToSpace,
                onOpenDownloadCache = rootNavController::navigateToDownload,
                onStartDownload = downloadViewModel::enqueueDownload,
            )

            loginScreen(
                onLoginSuccess = { rootNavController.popBackStack() },
                onBack = { rootNavController.popBackStack() },
                onSwitchToSms = {
                    rootNavController.navigate(SMS_LOGIN_ROUTE) {
                        popUpTo(LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )

            smsLoginScreen(
                onLoginSuccess = { rootNavController.popBackStack() },
                onBack = { rootNavController.popBackStack() },
                onSwitchToQr = {
                    rootNavController.navigate(LOGIN_ROUTE) {
                        popUpTo(SMS_LOGIN_ROUTE) { inclusive = true }
                    }
                }
            )

            accountScreen(
                onBack = { rootNavController.popBackStack() },
                onAddAccount = { rootNavController.navigate(SMS_LOGIN_ROUTE) },
                onSwitched = { rootNavController.popBackStack() }
            )

            settingsScreen(rootNavController)
            searchScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = rootNavController::navigateToSpace,
                onOpenVideo = openVideo
            )
            historyScreen(
                onBack = { rootNavController.popBackStack() },
                onSearch = { tab -> rootNavController.navigateToHistorySearch(tab) },
                onOpenHistoryTarget = openHistoryTarget
            )
            historySearchScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenHistoryTarget = openHistoryTarget
            )
            watchLaterScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenVideo = openVideo
            )
            favoriteScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenContent = { target ->
                    when (target) {
                        is FavoriteContentTarget.Video -> openVideo(target.target)
                        is FavoriteContentTarget.DynamicDetail -> {
                            rootNavController.navigateToDynamicDetail(target.opusId)
                        }
                    }
                },
                onOpenFolder = { fid ->
                    rootNavController.navigateToFavoriteFolder(fid)
                }
            )
            composable(HOME_INTEREST_ROUTE) {
                InterestScreen(onBack = { rootNavController.popBackStack() })
            }
            spaceScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenVideo = openVideo,
                onOpenDynamic = { opusId ->
                    rootNavController.navigateToDynamicDetail(opusId)
                },
                onOpenLive = openLive,
                onOpenIm = { mid, name, avatar ->
                    rootNavController.navigateToImConversation(
                        ImSessionItem(
                            key = "space:$mid",
                            talkerId = mid,
                            sessionType = 1,
                            name = name,
                            avatar = avatar
                        )
                    )
                },
                onOpenRelation = { vmid, initialTab ->
                    rootNavController.navigateToSpaceRelation(vmid, initialTab)
                }
            )
            spaceRelationScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = rootNavController::navigateToSpace
            )
            downloadScreen(
                navController = rootNavController,
                onBack = { rootNavController.popBackStack() },
                viewModel = downloadViewModel,
                closePlaybackHost = closeVideoHost
            )

            dynamicDetailScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = { route ->
                    rootNavController.popBackStack()
                    rootNavController.navigateToSpace(route)
                }
            )

            listenDetailScreen(
                onBack = { rootNavController.popBackStack() }
            )

            imConversationScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = rootNavController::navigateToSpace,
                onOpenVideo = openVideo
            )

            imMsgFeedScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = rootNavController::navigateToSpace,
                onOpenVideoDetail = openVideo,
                onOpenDynamicDetail = rootNavController::navigateToDynamicDetail,
                onOpenLiveDetail = openLive
            )
        }

        PlaybackHost(
            mode = hostMode,
            playbackHostViewModel = playbackHostViewModel,
            onExpand = {
                when (val target = playbackHostViewModel.currentTarget.value) {
                    is StreamPlaybackTarget.Video -> openVideo(target.target)
                    is StreamPlaybackTarget.Live -> playbackHostViewModel.expand()
                    null -> Unit
                }
            },
            onTogglePlay = playbackHostViewModel::togglePlayPause,
            onClose = {
                when (playbackHostViewModel.currentTarget.value) {
                    is StreamPlaybackTarget.Video -> closeVideoHost()
                    is StreamPlaybackTarget.Live -> playbackHostViewModel.close()
                    null -> Unit
                }
            },
            onDismissExpanded = dismissPlaybackHost,
            liveViewModel = liveViewModel
        )
    }
}

@Composable
private fun MainTabsScaffold(
    currentTab: TopLevelRoute,
    onTabChange: (TopLevelRoute) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateFromUser: (UserDest) -> Unit,
    onNavigateToDownload: () -> Unit,
    onNavigateToVideo: (VideoTarget) -> Unit,
    onNavigateToSpace: (SpaceRoute) -> Unit,
    onNavigateToLive: (LiveRoute) -> Unit,
    onNavigateToArticle: (String, Int) -> Unit,
    onNavigateToDynamicDetail: (String) -> Unit,
    onNavigateToListenDetail: (Long, Int, Long, String, String, String) -> Unit,
    onNavigateToMsgFeed: () -> Unit,
    onNavigateToImConversation: (ImSessionItem) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    var homeRefreshRequest by remember { mutableIntStateOf(0) }
    var dynamicRefreshRequest by remember { mutableIntStateOf(0) }
    var messageRefreshRequest by remember { mutableIntStateOf(0) }
    val selectTab: (TopLevelRoute) -> Unit = { tab ->
        if (tab != currentTab) {
            when (currentTab) {
                TopLevelRoute.HOME -> homeRefreshRequest = 0
                TopLevelRoute.DYNAMIC -> dynamicRefreshRequest = 0
                TopLevelRoute.MESSAGE -> messageRefreshRequest = 0
                TopLevelRoute.PROFILE -> Unit
            }
            onTabChange(tab)
        } else {
            when (tab) {
                TopLevelRoute.HOME -> homeRefreshRequest++
                TopLevelRoute.DYNAMIC -> dynamicRefreshRequest++
                TopLevelRoute.MESSAGE -> messageRefreshRequest++
                TopLevelRoute.PROFILE -> Unit
            }
        }
    }
    val userViewModel: UserViewModel = hiltViewModel()
    val userState by userViewModel.uiState.collectAsStateWithLifecycle()
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val useStartAlignedBottomToolbar = windowSizeClass.isWidthAtLeastBreakpoint(600)
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val fixBottomBar by settingsViewModel.fixBottomBar.collectAsStateWithLifecycle()
    val navVisibilityController = rememberTopLevelNavVisibilityController(fixBottomBar)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(navVisibilityController.connection)
    ) {
        TopLevelRoute.entries.forEach { tab ->
            if (currentTab == tab) {
                saveableStateHolder.SaveableStateProvider(tab.route) {
                    when (tab) {
                        TopLevelRoute.HOME -> HomeScreen(
                            onNavigateToSearch = onNavigateToSearch,
                            onNavigateToProfile = { selectTab(TopLevelRoute.PROFILE) },
                            profileAvatar = userState.user?.avatar,
                            onOpenVideo = onNavigateToVideo,
                            onOpenSpace = onNavigateToSpace,
                            onOpenLive = onNavigateToLive,
                            onOpenDynamic = onNavigateToDynamicDetail,
                            onOpenArticle = onNavigateToArticle,
                            onOpenListenItem = onNavigateToListenDetail,
                            refreshRequest = homeRefreshRequest
                        )
                        TopLevelRoute.DYNAMIC -> DynamicScreen(
                            onOpenVideo = onNavigateToVideo,
                            onOpenSpace = onNavigateToSpace,
                            onOpenLive = onNavigateToLive,
                            onOpenDynamic = onNavigateToDynamicDetail,
                            refreshRequest = dynamicRefreshRequest
                        )
                        TopLevelRoute.MESSAGE -> ImScreen(
                            onOpenConversation = onNavigateToImConversation,
                            onOpenMsgFeed = onNavigateToMsgFeed,
                            refreshRequest = messageRefreshRequest
                        )
                        TopLevelRoute.PROFILE -> UserScreen(
                            onNavigateToAccount = onNavigateToAccount,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigate = onNavigateFromUser,
                            onNavigateToDownload = onNavigateToDownload,
                            onOpenSpace = onNavigateToSpace
                        )
                    }
                }
            }
        }

        TopLevelFloatingNavigation(
            modifier = Modifier
                .align(
                    if (useStartAlignedBottomToolbar) Alignment.BottomStart else Alignment.BottomCenter
                )
                .padding(
                    start = if (useStartAlignedBottomToolbar) topLevelNavEdgePadding else 0.dp,
                    bottom = topLevelNavEdgePadding
                )
                .zIndex(1f),
            currentTab = currentTab,
            visibilityController = navVisibilityController,
            onTabChange = selectTab,
            onNavigateToSearch = onNavigateToSearch
        )
    }
}

@Composable
private fun TopLevelFloatingNavigation(
    modifier: Modifier = Modifier,
    currentTab: TopLevelRoute,
    visibilityController: TopLevelNavVisibilityController,
    onTabChange: (TopLevelRoute) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val toolbarShape = MaterialTheme.shapes.extraLarge
    val edgePaddingPx = with(LocalDensity.current) { topLevelNavEdgePadding.toPx() }
    var hiddenDistancePx by remember { mutableFloatStateOf(0f) }
    val animatedOffsetPx by animateFloatAsState(
        targetValue = if (visibilityController.hidden) hiddenDistancePx + edgePaddingPx else 0f,
        animationSpec = tween(durationMillis = topLevelNavAnimationDurationMillis),
        label = "top level nav offset"
    )
    val animatedModifier = modifier
        .onSizeChanged { size ->
            hiddenDistancePx = size.height.toFloat()
        }
        .graphicsLayer {
            translationY = animatedOffsetPx
        }
    val tabs: @Composable () -> Unit = {
        TopLevelRoute.entries.forEach { tab ->
            TopLevelFloatingNavigationItem(
                tab = tab,
                selected = currentTab == tab,
                onClick = { onTabChange(tab) }
            )
        }
    }
    val searchFab: @Composable () -> Unit = {
        FloatingActionButton(
            onClick = onNavigateToSearch,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "搜索"
            )
        }
    }

    Row(
        modifier = animatedModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = toolbarShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp), content = { tabs() })
        }
        Spacer(modifier = Modifier.width(topLevelNavGap))
        searchFab()
    }
}

@Composable
private fun TopLevelFloatingNavigationItem(
    tab: TopLevelRoute,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        FilledIconButton(onClick = onClick) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label
            )
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Stable
private class TopLevelNavVisibilityController(
    thresholdPx: Float,
    initialHidden: Boolean = false,
    private val fixBottomBar: Boolean = false
) {
    private val triggerThresholdPx = thresholdPx
    private var accumulatedScroll = 0f

    var hidden by mutableStateOf(initialHidden)
        private set

    val connection = object : NestedScrollConnection {
        @Suppress("SameReturnValue")
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (fixBottomBar) return Offset.Zero
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            if (available.y == 0f) return Offset.Zero
            if (kotlin.math.abs(available.y) < kotlin.math.abs(available.x)) return Offset.Zero
            updateVisibility(available.y)
            return Offset.Zero
        }
    }

    private fun updateVisibility(deltaY: Float) {
        if (deltaY == 0f) return
        if ((deltaY < 0f && accumulatedScroll > 0f) || (deltaY > 0f && accumulatedScroll < 0f)) {
            accumulatedScroll = 0f
        }
        accumulatedScroll += deltaY
        when {
            accumulatedScroll <= -triggerThresholdPx -> {
                if (!hidden) {
                    hidden = true
                }
                accumulatedScroll = 0f
            }
            accumulatedScroll >= triggerThresholdPx -> {
                if (hidden) {
                    hidden = false
                }
                accumulatedScroll = 0f
            }
        }
    }
}

@Composable
private fun rememberTopLevelNavVisibilityController(
    fixBottomBar: Boolean = false
): TopLevelNavVisibilityController {
    val thresholdPx = with(LocalDensity.current) { topLevelNavScrollThreshold.toPx() }
    return remember(thresholdPx, fixBottomBar) {
        TopLevelNavVisibilityController(thresholdPx = thresholdPx, fixBottomBar = fixBottomBar)
    }
}
