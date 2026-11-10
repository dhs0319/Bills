package com.dhs0319.bills.navigation

import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.tappableElement
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import com.dhs0319.bills.core.designsystem.theme.LocalTopLevelNavSpace
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlinedfilled.Search
import com.dhs0319.bills.core.designsystem.component.roundScreenSafePadding
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.theme.ThemeConfig
import com.dhs0319.bills.core.designsystem.theme.ThemeMode
import com.dhs0319.bills.core.designsystem.theme.buildNavTransitions
import com.dhs0319.bills.core.model.FavoriteContentTarget
import com.dhs0319.bills.core.model.HistoryTarget
import com.dhs0319.bills.core.model.ImSessionItem
import com.dhs0319.bills.core.model.ImUnreadBadge
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.StreamPlaybackTarget
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
import com.dhs0319.bills.feature.im.navigation.imNewFollowersScreen
import com.dhs0319.bills.feature.im.navigation.imReceivedLikesScreen
import com.dhs0319.bills.feature.im.navigation.navigateToImConversation
import com.dhs0319.bills.feature.im.navigation.navigateToMsgFeed
import com.dhs0319.bills.feature.im.navigation.navigateToNewFollowers
import com.dhs0319.bills.feature.im.navigation.navigateToReceivedLikes
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
private const val PENDING_CONVERSATION_KEY = "pending_conversation"
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
    val navigationViewModel: AppNavigationViewModel = hiltViewModel()
    val openLogin: () -> Unit = {
        rootNavController.navigate(SMS_LOGIN_ROUTE) { launchSingleTop = true }
    }
    val openConversation: (ImSessionItem) -> Unit = openConversation@{ item ->
        val talkerId = item.talkerId ?: return@openConversation
        val sessionType = item.sessionType ?: return@openConversation
        if (navigationViewModel.isLoggedIn) {
            rootNavController.navigateToImConversation(item)
        } else {
            openLogin()
            // Keep the destination with this login entry so cancelling login discards it.
            rootNavController.currentBackStackEntry?.savedStateHandle?.set(
                PENDING_CONVERSATION_KEY,
                bundleOf(
                    "talkerId" to talkerId,
                    "sessionType" to sessionType,
                    "name" to item.name,
                    "avatar" to item.avatar,
                    "autoReply" to item.latestMessageIsAutoReply
                )
            )
        }
    }
    val completeLogin: () -> Unit = {
        val pending = rootNavController.currentBackStackEntry?.savedStateHandle
            ?.remove<Bundle>(PENDING_CONVERSATION_KEY)
        rootNavController.popBackStack()
        if (pending != null && navigationViewModel.isLoggedIn) {
            val talkerId = pending.getLong("talkerId")
            rootNavController.navigateToImConversation(
                ImSessionItem(
                    key = "login:$talkerId",
                    talkerId = talkerId,
                    sessionType = pending.getInt("sessionType"),
                    name = pending.getString("name").orEmpty(),
                    avatar = pending.getString("avatar"),
                    latestMessageIsAutoReply = pending.getBoolean("autoReply")
                )
            )
        }
    }
    val switchLoginMethod: (String, String) -> Unit = { fromRoute, toRoute ->
        val pending = rootNavController.currentBackStackEntry?.savedStateHandle
            ?.get<Bundle>(PENDING_CONVERSATION_KEY)
        rootNavController.navigate(toRoute) {
            popUpTo(fromRoute) { inclusive = true }
        }
        if (pending != null) {
            rootNavController.currentBackStackEntry?.savedStateHandle
                ?.set(PENDING_CONVERSATION_KEY, pending)
        }
    }
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
                    themeMode = themeConfig.themeMode,
                    onTabChange = { currentTab = it },
                    onNavigateToSearch = { rootNavController.navigateToSearch() },
                    onNavigateToSettings = { rootNavController.navigate(SETTINGS_ROUTE) },
                    onNavigateToAccount = { rootNavController.navigate(ACCOUNT_ROUTE) },
                    onNavigateToLogin = openLogin,
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
                    onNavigateToReceivedLikes = { rootNavController.navigateToReceivedLikes() },
                    onNavigateToNewFollowers = { rootNavController.navigateToNewFollowers() },
                    onNavigateToImConversation = openConversation
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
                onLoginSuccess = completeLogin,
                onBack = { rootNavController.popBackStack() },
                onSwitchToSms = {
                    switchLoginMethod(LOGIN_ROUTE, SMS_LOGIN_ROUTE)
                }
            )

            smsLoginScreen(
                onLoginSuccess = completeLogin,
                onBack = { rootNavController.popBackStack() },
                onSwitchToQr = {
                    switchLoginMethod(SMS_LOGIN_ROUTE, LOGIN_ROUTE)
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
                onOpenHistoryTarget = openHistoryTarget,
                onLogin = openLogin
            )
            historySearchScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenHistoryTarget = openHistoryTarget
            )
            watchLaterScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenVideo = openVideo,
                onLogin = openLogin
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
                onOpenFolder = { folder ->
                    rootNavController.navigateToFavoriteFolder(folder)
                },
                onLogin = openLogin
            )
            composable(HOME_INTEREST_ROUTE) {
                InterestScreen(
                    onBack = { rootNavController.popBackStack() },
                    onLogin = openLogin
                )
            }
            spaceScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenVideo = openVideo,
                onOpenDynamic = { opusId ->
                    rootNavController.navigateToDynamicDetail(opusId)
                },
                onOpenLive = openLive,
                onOpenIm = { mid, name, avatar ->
                    openConversation(
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

            imReceivedLikesScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = rootNavController::navigateToSpace,
                onOpenVideoDetail = openVideo,
                onOpenDynamicDetail = rootNavController::navigateToDynamicDetail,
                onOpenLiveDetail = openLive
            )

            imNewFollowersScreen(
                onBack = { rootNavController.popBackStack() },
                onOpenSpace = rootNavController::navigateToSpace
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
    themeMode: ThemeMode,
    onTabChange: (TopLevelRoute) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateFromUser: (UserDest) -> Unit,
    onNavigateToDownload: () -> Unit,
    onNavigateToVideo: (VideoTarget) -> Unit,
    onNavigateToSpace: (SpaceRoute) -> Unit,
    onNavigateToLive: (LiveRoute) -> Unit,
    onNavigateToArticle: (String, Int) -> Unit,
    onNavigateToDynamicDetail: (String) -> Unit,
    onNavigateToListenDetail: (Long, Int, Long, String, String, String) -> Unit,
    onNavigateToMsgFeed: () -> Unit,
    onNavigateToReceivedLikes: () -> Unit,
    onNavigateToNewFollowers: () -> Unit,
    onNavigateToImConversation: (ImSessionItem) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    var homeRefreshRequest by remember { mutableIntStateOf(0) }
    var dynamicRefreshRequest by remember { mutableIntStateOf(0) }
    var messageRefreshRequest by remember { mutableIntStateOf(0) }
    val messageUnreadViewModel: MessageUnreadViewModel = hiltViewModel()
    val selectTab: (TopLevelRoute) -> Unit = { tab ->
        if (tab == TopLevelRoute.MESSAGE) {
            messageUnreadViewModel.dismissBadge()
        }
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
    val messageUnreadBadge by messageUnreadViewModel.badge.collectAsStateWithLifecycle()
    LifecycleStartEffect(messageUnreadViewModel) {
        messageUnreadViewModel.setActive(true)
        onStopOrDispose { messageUnreadViewModel.setActive(false) }
    }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val tappableInsets = WindowInsets.tappableElement
    val hasTappableNavigationBar = tappableInsets.getBottom(density) > 0 ||
        tappableInsets.getLeft(density, layoutDirection) > 0 ||
        tappableInsets.getRight(density, layoutDirection) > 0
    val navigationBarPadding = if (hasTappableNavigationBar) {
        WindowInsets.navigationBars
    } else {
        WindowInsets(0)
    }
    val useStartAlignedBottomToolbar = configuration.screenWidthDp >= 600
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val fixBottomBar by settingsViewModel.fixBottomBar.collectAsStateWithLifecycle()
    val useBottomMenu by settingsViewModel.useBottomMenu.collectAsStateWithLifecycle()
    val navVisibilityController = rememberTopLevelNavVisibilityController(fixBottomBar)
    var topLevelNavSpace by remember(useBottomMenu, isLandscape) { mutableStateOf(0.dp) }
    val bottomContentPadding by animateDpAsState(
        targetValue = if (!isLandscape && useBottomMenu && !navVisibilityController.hidden) {
            topLevelNavSpace
        } else {
            0.dp
        },
        animationSpec = tween(durationMillis = topLevelNavAnimationDurationMillis),
        label = "bottom menu content padding"
    )

    val tabContent: @Composable () -> Unit = {
        TopLevelRoute.entries.forEach { tab ->
            if (currentTab == tab) {
                saveableStateHolder.SaveableStateProvider(tab.route) {
                    when (tab) {
                        TopLevelRoute.HOME -> HomeScreen(
                            onNavigateToSearch = onNavigateToSearch,
                            onNavigateToProfile = { selectTab(TopLevelRoute.PROFILE) },
                            profileAvatar = userState.user?.avatar,
                            showTopActions = !isLandscape,
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
                            onLogin = onNavigateToLogin,
                            refreshRequest = dynamicRefreshRequest
                        )
                        TopLevelRoute.MESSAGE -> ImScreen(
                            onOpenConversation = onNavigateToImConversation,
                            onLogin = onNavigateToLogin,
                            onOpenMsgFeed = onNavigateToMsgFeed,
                            onOpenReceivedLikes = onNavigateToReceivedLikes,
                            onOpenNewFollowers = onNavigateToNewFollowers,
                            refreshRequest = messageRefreshRequest
                        )
                        TopLevelRoute.PROFILE -> UserScreen(
                            onNavigateToAccount = onNavigateToAccount,
                            onNavigateToSettings = onNavigateToSettings,
                            onNavigate = onNavigateFromUser,
                            onNavigateToDownload = onNavigateToDownload,
                            themeMode = themeMode,
                            onThemeModeChange = settingsViewModel::updateThemeMode,
                            onOpenSpace = onNavigateToSpace
                        )
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(
                    navigationBarPadding.only(
                        WindowInsetsSides.End + WindowInsetsSides.Bottom
                    )
                )
        ) {
            TopLevelSideNavigation(
                currentTab = currentTab,
                messageUnreadBadge = messageUnreadBadge,
                profileAvatar = userState.user?.avatar,
                onTabChange = selectTab,
                onNavigateToSearch = onNavigateToSearch
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                content = { tabContent() }
            )
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(
                    navigationBarPadding.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .nestedScroll(navVisibilityController.connection)
        ) {
            CompositionLocalProvider(
                LocalTopLevelNavSpace provides if (useBottomMenu) 0.dp else topLevelNavSpace
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomContentPadding)
                        .consumeWindowInsets(PaddingValues(bottom = bottomContentPadding))
                        .clipToBounds()
                ) {
                    tabContent()
                }
            }
            if (useBottomMenu) {
                TopLevelBottomNavigation(
                    modifier = Modifier.align(Alignment.BottomCenter).zIndex(1f),
                    currentTab = currentTab,
                    messageUnreadBadge = messageUnreadBadge,
                    hidden = navVisibilityController.hidden,
                    onTabChange = selectTab,
                    onHeightChanged = { topLevelNavSpace = it }
                )
            } else {
                TopLevelFloatingNavigation(
                    modifier = Modifier
                        .align(
                            if (useStartAlignedBottomToolbar) {
                                Alignment.BottomStart
                            } else {
                                Alignment.BottomCenter
                            }
                        )
                        .zIndex(1f),
                    currentTab = currentTab,
                    messageUnreadBadge = messageUnreadBadge,
                    visibilityController = navVisibilityController,
                    startPadding = if (useStartAlignedBottomToolbar) topLevelNavEdgePadding else 0.dp,
                    onTabChange = selectTab,
                    onNavigateToSearch = onNavigateToSearch,
                    onHeightChanged = { topLevelNavSpace = it }
                )
            }
        }
    }
}

@Composable
private fun TopLevelSideNavigation(
    currentTab: TopLevelRoute,
    messageUnreadBadge: ImUnreadBadge,
    profileAvatar: String?,
    onTabChange: (TopLevelRoute) -> Unit,
    onNavigateToSearch: () -> Unit
) {
    val layoutDirection = LocalLayoutDirection.current
    val startSafeInset = WindowInsets.safeDrawing
        .asPaddingValues()
        .calculateStartPadding(layoutDirection)
    Surface(
        modifier = Modifier
            .width(112.dp + startSafeInset)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Start + WindowInsetsSides.Top
                    )
                )
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = { onTabChange(TopLevelRoute.PROFILE) }) {
                AvatarImage(
                    url = profileAvatar,
                    contentDescription = TopLevelRoute.PROFILE.label,
                    modifier = Modifier.size(44.dp)
                )
            }
            IconButton(onClick = onNavigateToSearch) {
                Icon(
                    imageVector = MaterialSymbols.OutlinedFilled.Search,
                    contentDescription = "搜索",
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            TopLevelRoute.entries.forEach { tab ->
                TopLevelSideNavigationItem(
                    tab = tab,
                    selected = currentTab == tab,
                    messageUnreadBadge = messageUnreadBadge,
                    onClick = { onTabChange(tab) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TopLevelSideNavigationItem(
    tab: TopLevelRoute,
    selected: Boolean,
    messageUnreadBadge: ImUnreadBadge,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelNavigationIcon(
                tab = tab,
                selected = selected,
                messageUnreadBadge = messageUnreadBadge,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = tab.label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun TopLevelFloatingNavigation(
    modifier: Modifier = Modifier,
    currentTab: TopLevelRoute,
    messageUnreadBadge: ImUnreadBadge,
    visibilityController: TopLevelNavVisibilityController,
    startPadding: Dp,
    onTabChange: (TopLevelRoute) -> Unit,
    onNavigateToSearch: () -> Unit,
    onHeightChanged: (Dp) -> Unit = {}
) {
    val toolbarShape = MaterialTheme.shapes.extraLarge
    val density = LocalDensity.current
    var hiddenDistancePx by remember { mutableFloatStateOf(0f) }
    val animatedOffsetPx by animateFloatAsState(
        targetValue = if (visibilityController.hidden) hiddenDistancePx else 0f,
        animationSpec = tween(durationMillis = topLevelNavAnimationDurationMillis),
        label = "top level nav offset"
    )
    val animatedModifier = modifier
        .onSizeChanged { size ->
            hiddenDistancePx = size.height.toFloat()
            onHeightChanged(with(density) { size.height.toDp() })
        }
        .graphicsLayer {
            translationY = animatedOffsetPx
        }
        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
        .padding(start = startPadding, bottom = topLevelNavEdgePadding)
    val tabs: @Composable () -> Unit = {
        TopLevelRoute.entries.forEach { tab ->
            TopLevelFloatingNavigationItem(
                tab = tab,
                selected = currentTab == tab,
                messageUnreadBadge = messageUnreadBadge,
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
                imageVector = MaterialSymbols.OutlinedFilled.Search,
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
    messageUnreadBadge: ImUnreadBadge,
    onClick: () -> Unit
) {
    TopLevelNavigationIcon(
        tab = tab,
        selected = selected,
        messageUnreadBadge = messageUnreadBadge,
        tint = if (selected) null else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    )
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
