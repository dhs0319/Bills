package com.dhs0319.bills.feature.video.detail

import android.content.ComponentName
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyLayoutScrollScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Add
import com.composables.icons.materialsymbols.outlined.Chat_bubble
import com.composables.icons.materialsymbols.outlined.Chevron_right
import com.composables.icons.materialsymbols.outlined.Download
import com.composables.icons.materialsymbols.outlined.Movie
import com.composables.icons.materialsymbols.outlined.Open_in_new
import com.composables.icons.materialsymbols.outlined.Play_circle
import com.composables.icons.materialsymbols.outlined.Star as SymbolOutlinedStar
import com.composables.icons.materialsymbols.outlined.Subtitles
import com.composables.icons.materialsymbols.outlined.Thumb_down as SymbolOutlinedThumbdown
import com.composables.icons.materialsymbols.outlined.Thumb_up as SymbolOutlinedThumbup
import com.composables.icons.materialsymbols.outlinedfilled.Check
import com.composables.icons.materialsymbols.outlinedfilled.Keyboard_arrow_down
import com.composables.icons.materialsymbols.outlinedfilled.Keyboard_arrow_up
import com.composables.icons.materialsymbols.outlinedfilled.Star as SymbolOutlinedFilledStar
import com.composables.icons.materialsymbols.outlinedfilled.Thumb_down as SymbolOutlinedFilledThumbdown
import com.composables.icons.materialsymbols.outlinedfilled.Thumb_up as SymbolOutlinedFilledThumbup
import com.dhs0319.bills.core.common.compactCount
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.designsystem.component.AnimatedLiveBadge
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.BiliImageVariant
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCard
import com.dhs0319.bills.core.designsystem.component.HorizontalVideoCardSkeleton
import com.dhs0319.bills.core.designsystem.component.PagerSlidingTabRow
import com.dhs0319.bills.core.designsystem.component.SlidingTabLayout
import com.dhs0319.bills.core.designsystem.component.SkeletonBlock
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.UploaderBadge
import com.dhs0319.bills.core.designsystem.component.VideoDetailInfoSkeleton
import com.dhs0319.bills.core.designsystem.component.WatchLaterMoreMenu
import com.dhs0319.bills.core.designsystem.component.copyTextOnLongPress
import com.dhs0319.bills.core.designsystem.component.horizontalVideoCardSkeletonHeight
import com.dhs0319.bills.core.designsystem.icon.AppIcons
import com.dhs0319.bills.core.model.CommentSubject
import com.dhs0319.bills.core.model.QualityOption
import com.dhs0319.bills.core.model.ResolvedVideoIds
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.SpaceRouteTool
import com.dhs0319.bills.core.model.VideoDetail
import com.dhs0319.bills.core.model.VideoOwner
import com.dhs0319.bills.core.model.VideoPagePart
import com.dhs0319.bills.core.model.VideoRelate
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.core.model.VideoSeason
import com.dhs0319.bills.core.model.VideoSeasonEpisode
import com.dhs0319.bills.core.model.VideoStaff
import com.dhs0319.bills.core.model.VideoStat
import com.dhs0319.bills.feature.comment.CommentPanel
import com.dhs0319.bills.feature.video.formatDuration
import com.dhs0319.bills.core.video.VideoFavoriteFolder
import com.dhs0319.bills.feature.video.action.VideoActionUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.ceil
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun VideoDetailPage(
    summaryModifier: Modifier,
    tabsModifier: Modifier,
    isLandscape: Boolean,
    isExpandedWidth: Boolean,
    detail: VideoDetail?,
    detailPlaceholder: VideoDetail?,
    ids: ResolvedVideoIds,
    detailLoading: Boolean,
    detailError: String?,
    detailConnectionFailed: Boolean,
    actionState: VideoActionUiState,
    commentSubject: CommentSubject?,
    onOpenVideo: (VideoTarget) -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    loadSeasonPublishTimes: suspend (Long, Long) -> Map<Long, Long>,
    loadPageCovers: suspend (Long) -> Map<Long, String>,
    onOpenSpace: (SpaceRoute) -> Unit,
    onDownloadClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onToggleFollow: (Long) -> Unit,
    isSelf: (Long) -> Boolean,
    isLoggedIn: () -> Boolean,
    onSubmitCoins: (Int, () -> Unit) -> Unit,
    onLoadFavoriteFolders: ((List<VideoFavoriteFolder>) -> Unit) -> Unit,
    onSaveFavoriteFolders: (Set<Long>, Set<Long>, () -> Unit) -> Unit,
    onOpenEpisode: (VideoTarget) -> Unit,
    onSwitchPage: (Long) -> Unit
) {
    val aid = ids.aid
    val aidKey = aid.takeIf { it > 0L }
    val curCid = ids.cid.takeIf { it > 0L }
    var infoExpanded by key(aidKey) { rememberSaveable { mutableStateOf(false) } }
    var sheet by remember(aidKey) { mutableStateOf<DetailSheet?>(null) }
    // Only the portrait list has summary and playlist items above the related videos.
    val portraitDetailListState = key(aidKey) { rememberLazyListState() }
    val landscapeRelatedListState = key(aidKey) { rememberLazyListState() }
    val detailListState = if (isLandscape) landscapeRelatedListState else portraitDetailListState
    val commentListState = key(aidKey) { rememberLazyListState() }
    val commentThreadListState = key(aidKey) { rememberLazyListState() }
    val season = remember(detail?.season) {
        detail?.season?.takeIf { it.sections.any { section -> section.eps.isNotEmpty() } }
    }
    val hasPlaylist = season != null || (detail?.pages?.size ?: 0) > 1
    val showPlaylist = isLandscape && hasPlaylist
    val detailTabs = remember(isLandscape, showPlaylist) {
        when {
            showPlaylist -> listOf("相关视频", "播放列表", "评论")
            isLandscape -> listOf("相关视频", "评论")
            else -> listOf("简介", "评论")
        }
    }
    val commentsPage = detailTabs.lastIndex
    val pagerState = rememberPagerState(pageCount = { detailTabs.size })
    var pagerAid by rememberSaveable { mutableLongStateOf(ids.aid) }
    if (pagerAid != ids.aid) {
        SideEffect {
            pagerAid = ids.aid
            // Keep the playlist open when choosing another video in landscape.
            if (!isLandscape) pagerState.requestScrollToPage(0)
        }
    }
    val contentHorizontalPad = if (isLandscape || isExpandedWidth) 0.dp else 16.dp
    val relatedCardHorizontalPad = contentHorizontalPad.coerceAtLeast(6.dp)
    val commentHorizontalPad = if (isLandscape) relatedCardHorizontalPad else contentHorizontalPad
    val scope = rememberCoroutineScope()
    var tabScrollJob by remember(aidKey) { mutableStateOf<Job?>(null) }
    var coinSheetVisible by remember(aidKey) { mutableStateOf(false) }
    var selectedCoinAmount by remember(aidKey) { mutableStateOf(1) }
    var favoriteSheetVisible by remember(aidKey) { mutableStateOf(false) }
    var favoriteFolders by remember(aidKey) { mutableStateOf(emptyList<VideoFavoriteFolder>()) }
    var originalFavoriteFolderIds by remember(aidKey) { mutableStateOf(emptySet<Long>()) }
    var selectedFavoriteFolderIds by remember(aidKey) { mutableStateOf(emptySet<Long>()) }
    var shareSheetVisible by remember(aidKey) { mutableStateOf(false) }
    var teamSheetVisible by remember(aidKey) { mutableStateOf(false) }
    var unfollowMid by remember(aidKey) { mutableStateOf<Long?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val shareUrl = remember(detail, ids) { detail?.let { buildVideoShareUrl(ids) } }

    val displayedDetail = remember(detail, detailPlaceholder) {
        if (detail == null || detailPlaceholder == null) return@remember detail

        val owner = detail.owner
        val previousOwner = detailPlaceholder.owner
        val displayedOwner = if (
            owner != null && previousOwner != null && owner.mid > 0L && owner.mid == previousOwner.mid
        ) {
            owner.copy(
                fansText = owner.fansText ?: previousOwner.fansText,
                arcCountText = owner.arcCountText ?: previousOwner.arcCountText,
                face = owner.face ?: previousOwner.face
            )
        } else {
            owner
        }
        detail.copy(
            owner = displayedOwner,
            stat = detail.stat ?: detailPlaceholder.stat,
            pubTs = detail.pubTs ?: detailPlaceholder.pubTs,
            desc = detail.desc.ifBlank { detailPlaceholder.desc },
            tags = detail.tags.ifEmpty { detailPlaceholder.tags }
        )
    }
    val replyCount = displayedDetail?.stat?.reply
    val detailTabBadges = remember(detailTabs, replyCount) {
        val badge = replyCount?.takeIf(String::isNotBlank)?.let { compactCount(it) }
        List(detailTabs.size) { page -> if (page == commentsPage) badge else null }
    }
    val latestActionState by rememberUpdatedState(actionState)
    val onFollowClick: (Long) -> Unit = { mid ->
        val actions = latestActionState
        if (!isLoggedIn()) {
            Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
        } else if (actions.aid == aid) {
            if (actions.followingByMid[mid] == true) {
                unfollowMid = mid
            } else {
                onToggleFollow(mid)
            }
        }
    }
    val onOpenTeamMember: (VideoStaff) -> Unit = { member ->
        teamSheetVisible = false
        onOpenSpace(SpaceRoute(mid = member.mid, name = member.name, fromViewAid = aidKey))
    }

    val summary: @Composable (Modifier) -> Unit = { modifier ->
        VideoSummarySection(
            modifier = modifier,
            isLandscape = isLandscape,
            detail = displayedDetail,
            ids = ids,
            loading = detailLoading,
            error = detailError,
            connectionFailed = detailConnectionFailed,
            actionState = actionState,
            infoExpanded = infoExpanded,
            onToggleInfo = { infoExpanded = !infoExpanded },
            onOpenComments = { scope.launch { pagerState.animateScrollToPage(commentsPage) } },
            onOpenSpace = onOpenSpace,
            onOpenTeam = { teamSheetVisible = true },
            onOpenTeamMember = onOpenTeamMember,
            onDownloadClick = onDownloadClick,
            onToggleLike = onToggleLike,
            onToggleDislike = onToggleDislike,
            onFollowClick = onFollowClick,
            isSelf = isSelf,
            onOpenCoinPicker = {
                if (!isLoggedIn()) {
                    Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                } else if (actionState.initialized && actionState.aid > 0L && !actionState.coinBusy) {
                    selectedCoinAmount = 1
                    coinSheetVisible = true
                }
            },
            onOpenFavoritePicker = {
                if (
                    actionState.initialized &&
                    actionState.aid > 0L &&
                    !actionState.favoriteLoading &&
                    !actionState.favoriteSaving
                ) {
                    onLoadFavoriteFolders { folders ->
                        val selectedIds = folders.filter(VideoFavoriteFolder::selected)
                            .mapTo(linkedSetOf(), VideoFavoriteFolder::id)
                        favoriteFolders = folders
                        originalFavoriteFolderIds = selectedIds
                        selectedFavoriteFolderIds = selectedIds
                        favoriteSheetVisible = true
                    }
                }
            },
            onOpenShare = {
                if (shareUrl == null) {
                    Toast.makeText(context, "视频链接无效", Toast.LENGTH_SHORT).show()
                } else {
                    shareSheetVisible = true
                }
            }
        )
    }
    val onShowPages = {
        sheet = detail?.pages?.takeIf { it.size > 1 }?.let { DetailSheet.Page(it, curCid) }
    }
    if (isLandscape) {
        summary(summaryModifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp))
    }
    Box(tabsModifier.fillMaxSize().clipToBounds()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (sheet != null) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
            PagerSlidingTabRow(
                tabs = detailTabs,
                badges = detailTabBadges,
                pagerState = pagerState,
                tabTextStyle = MaterialTheme.typography.titleSmall,
                layout = SlidingTabLayout.Content,
                onReselect = { page ->
                    if (page != 0 && page != commentsPage) return@PagerSlidingTabRow
                    tabScrollJob?.cancel()
                    tabScrollJob = scope.launch {
                        if (page == 0) {
                            detailListState.animateScrollToItem(0)
                        } else {
                            commentListState.animateContinuouslyToTop()
                        }
                    }
                },
                modifier = Modifier
                    .padding(horizontal = contentHorizontalPad)
            )

            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                key = { detailTabs[it] },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> RelatedVideoList(
                        detail = detail,
                        loading = detailLoading,
                        error = detailError,
                        connectionFailed = detailConnectionFailed,
                        state = detailListState,
                        horizontalPad = contentHorizontalPad,
                        cardHorizontalPad = relatedCardHorizontalPad,
                        isLandscape = isLandscape,
                        onOpenVideo = onOpenVideo,
                        onAddToWatchLater = onAddToWatchLater
                    ) { itemModifier ->
                        if (!isLandscape) {
                            if (!detailConnectionFailed) {
                                item(key = "summary", contentType = "summary") { summary(itemModifier) }
                            }
                            if (detail != null && !detailLoading && detailError.isNullOrBlank()) {
                                playlistEntries(
                                    detail = detail,
                                    curCid = curCid,
                                    modifier = itemModifier,
                                    onSwitchPage = onSwitchPage,
                                    onShowPages = onShowPages,
                                    onShowSeason = { sheet = detail.season?.let { DetailSheet.Season(it, ids) } }
                                )
                            }
                        }
                    }

                    commentsPage -> {
                        CommentPanel(
                            subject = commentSubject,
                            isActive = pagerState.currentPage == page,
                            onOpenSpace = onOpenSpace,
                            modifier = Modifier.fillMaxSize(),
                            listState = commentListState,
                            threadListState = commentThreadListState,
                            showCommentCount = false,
                            contentPadding = PaddingValues(
                                start = commentHorizontalPad,
                                top = 0.dp,
                                end = commentHorizontalPad,
                                bottom = 20.dp
                            )
                        )
                    }

                    else -> PlaylistPage(
                        detail = detail,
                        season = season,
                        ids = ids,
                        loadSeasonPublishTimes = loadSeasonPublishTimes,
                        loadPageCovers = loadPageCovers,
                        onAddToWatchLater = onAddToWatchLater,
                        onOpenEpisode = onOpenEpisode,
                        onSwitchPage = onSwitchPage,
                        onShowPages = onShowPages
                    )
                }
            }
        }

        sheet?.let { activeSheet ->
            DetailSelectionPanel(
                title = when (activeSheet) {
                    is DetailSheet.Season -> "合集 · ${activeSheet.season.title}"
                    is DetailSheet.Page -> "分P列表"
                },
                onDismiss = { sheet = null },
                containerColor = MaterialTheme.colorScheme.background
            ) { close ->
                when (activeSheet) {
                    is DetailSheet.Season -> {
                        SeasonSheetContent(
                            season = activeSheet.season,
                            ids = activeSheet.ids,
                            currentPubTs = detail?.pubTs,
                            loadPublishTimes = loadSeasonPublishTimes,
                            onAddToWatchLater = onAddToWatchLater,
                            onOpenEpisode = { route ->
                                close {
                                    onOpenEpisode(route)
                                }
                            }
                        )
                    }

                    is DetailSheet.Page -> {
                        PageSheetContent(
                            pages = activeSheet.pages,
                            curCid = activeSheet.curCid,
                            aid = ids.aid,
                            pubTs = detail?.pubTs,
                            loadCovers = loadPageCovers,
                            onSwitchPage = { cid ->
                                close {
                                    onSwitchPage(cid)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (favoriteSheetVisible) {
        FavoriteFolderSheet(
            folders = favoriteFolders,
            selectedFolderIds = selectedFavoriteFolderIds,
            saving = actionState.favoriteSaving,
            onSelectFolder = { folderId, selected ->
                selectedFavoriteFolderIds = if (selected) {
                    selectedFavoriteFolderIds + folderId
                } else {
                    selectedFavoriteFolderIds - folderId
                }
            },
            onDismiss = { favoriteSheetVisible = false },
            onConfirm = {
                onSaveFavoriteFolders(
                    originalFavoriteFolderIds,
                    selectedFavoriteFolderIds
                ) {
                    favoriteSheetVisible = false
                }
            }
        )
    }

    if (coinSheetVisible) {
        CoinSheet(
            selectedAmount = selectedCoinAmount,
            busy = actionState.coinBusy,
            onSelectAmount = { selectedCoinAmount = it },
            onDismiss = { coinSheetVisible = false },
            onConfirm = {
                onSubmitCoins(selectedCoinAmount) {
                    coinSheetVisible = false
                }
            }
        )
    }

    if (shareSheetVisible && detail != null && shareUrl != null) {
        ShareSheet(
            title = formatShareTitle(detail.title),
            url = shareUrl,
            onDismiss = { shareSheetVisible = false },
            onShare = { channel ->
                if (channel == ShareChannel.COPY_LINK) {
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("video_link", shareUrl))
                    Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                    shareSheetVisible = false
                } else {
                    val launched = launchShareTarget(context, channel, formatShareTitle(detail.title), shareUrl)
                    if (!launched) {
                        Toast.makeText(context, "未安装对应应用", Toast.LENGTH_SHORT).show()
                    } else {
                        shareSheetVisible = false
                    }
                }
            }
        )
    }

    val team = displayedDetail?.staffs.orEmpty()
    if (teamSheetVisible && team.isNotEmpty()) {
        VideoTeamSheet(
            members = team,
            followingByMid = actionState.followingByMid,
            followBusyMids = actionState.followBusyMids,
            isSelf = isSelf,
            onOpenMember = onOpenTeamMember,
            onFollowClick = onFollowClick,
            onDismiss = { teamSheetVisible = false }
        )
    }

    unfollowMid?.let { mid ->
        AlertDialog(
            onDismissRequest = { unfollowMid = null },
            title = { Text("取消关注") },
            text = { Text("确定要取消关注TA吗？") },
            dismissButton = {
                TextButton(onClick = { unfollowMid = null }) {
                    Text("取消")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        unfollowMid = null
                        onToggleFollow(mid)
                    }
                ) {
                    Text("取消关注")
                }
            }
        )
    }
}

@Composable
private fun RelatedVideoList(
    detail: VideoDetail?,
    loading: Boolean,
    error: String?,
    connectionFailed: Boolean,
    state: LazyListState,
    horizontalPad: Dp,
    cardHorizontalPad: Dp,
    isLandscape: Boolean,
    onOpenVideo: (VideoTarget) -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    header: LazyListScope.(Modifier) -> Unit
) {
    val isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    val showTwoColumns = isTablet && !isLandscape
    val relates = detail?.relates.orEmpty()
    val topPadding = if (detail?.staffs.isNullOrEmpty()) horizontalPad else 0.dp
    val relateItemMod = Modifier.padding(horizontal = cardHorizontalPad)
    val rowSpacing = 6.dp
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val columns = if (showTwoColumns) 2 else 1
        val relateCardWidth = ((maxWidth - cardHorizontalPad * 2 - 6.dp * (columns - 1)) / columns)
            .coerceAtLeast(0.dp)
        val skeletonRowCount = if (loading && isTablet) {
            val rowHeight = horizontalVideoCardSkeletonHeight(relateCardWidth, isTablet = true) + rowSpacing
            // An extra row covers list padding and pixel rounding.
            (ceil(maxHeight / rowHeight).toInt() + 1).coerceAtLeast(DETAIL_RELATE_SKELETON_ROWS)
        } else {
            DETAIL_RELATE_SKELETON_ROWS
        }
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topPadding, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            header(Modifier.padding(horizontal = horizontalPad))
            if (loading) {
                items(
                    count = skeletonRowCount,
                    key = { index -> "detail_loading_relate_$index" },
                    contentType = { "skeleton" }
                ) {
                    if (showTwoColumns) {
                        Row(
                            modifier = relateItemMod.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            repeat(2) {
                                HorizontalVideoCardSkeleton(
                                    modifier = Modifier.weight(1f),
                                    cardWidth = relateCardWidth,
                                    isTablet = isTablet
                                )
                            }
                        }
                    } else {
                        HorizontalVideoCardSkeleton(
                            modifier = relateItemMod,
                            cardWidth = relateCardWidth,
                            isTablet = isTablet,
                            compact = isLandscape && !isTablet
                        )
                    }
                }
            } else if (error.isNullOrBlank() && relates.isNotEmpty()) {
                if (showTwoColumns) {
                    items(
                        count = (relates.size + 1) / 2,
                        key = { row -> "relate_${relates[row * 2].target.aid}_${relates[row * 2].target.cid}" },
                        contentType = { "relate_pair" }
                    ) { row ->
                        Row(
                            modifier = relateItemMod.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (column in 0..1) {
                                val relate = relates.getOrNull(row * 2 + column)
                                if (relate == null) {
                                    Spacer(Modifier.weight(1f))
                                } else {
                                    RelateRow(relate, relateCardWidth, onOpenVideo, onAddToWatchLater, Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    items(
                        items = relates,
                        key = { "relate_${it.target.aid}_${it.target.cid}" },
                        contentType = { "relate" }
                    ) { relate ->
                        RelateRow(relate, relateCardWidth, onOpenVideo, onAddToWatchLater, relateItemMod)
                    }
                }
            } else if (isLandscape && !connectionFailed) {
                item { StateMessageCard(text = error ?: "暂无相关视频", isError = !error.isNullOrBlank()) }
            }
        }
    }
}

private fun LazyListScope.playlistEntries(
    detail: VideoDetail,
    curCid: Long?,
    modifier: Modifier,
    onSwitchPage: (Long) -> Unit,
    onShowPages: () -> Unit,
    onShowSeason: () -> Unit
) {
    if (detail.pages.size > 1) {
        item(key = "page_entry", contentType = "page_entry") {
            VideoPageSelector(
                pages = detail.pages,
                curCid = curCid,
                onSwitchPage = onSwitchPage,
                onShowAll = onShowPages,
                modifier = modifier.padding(bottom = 10.dp)
            )
        }
    }
    detail.season?.let { season ->
        item(key = "season_entry", contentType = "season_entry") {
            SeasonEntryCard(season, curCid, modifier.padding(bottom = 10.dp), onShowSeason)
        }
    }
}

@Composable
private fun PlaylistPage(
    detail: VideoDetail?,
    season: VideoSeason?,
    ids: ResolvedVideoIds,
    loadSeasonPublishTimes: suspend (Long, Long) -> Map<Long, Long>,
    loadPageCovers: suspend (Long) -> Map<Long, String>,
    onAddToWatchLater: (Long) -> Unit,
    onOpenEpisode: (VideoTarget) -> Unit,
    onSwitchPage: (Long) -> Unit,
    onShowPages: () -> Unit
) {
    val pages = detail?.pages.orEmpty()
    when {
        season != null -> Column(Modifier.fillMaxSize()) {
            if (pages.size > 1) {
                VideoPageSelector(pages, ids.cid, onSwitchPage, onShowPages, Modifier.padding(horizontal = 6.dp))
            }
            SeasonSheetContent(
                season = season,
                ids = ids,
                currentPubTs = detail?.pubTs,
                loadPublishTimes = loadSeasonPublishTimes,
                onAddToWatchLater = onAddToWatchLater,
                onOpenEpisode = onOpenEpisode
            )
        }
        pages.size > 1 -> PageSheetContent(
            pages = pages,
            curCid = ids.cid,
            aid = ids.aid,
            pubTs = detail?.pubTs,
            loadCovers = loadPageCovers,
            onSwitchPage = onSwitchPage
        )
    }
}

@Composable
private fun VideoSummarySection(
    detail: VideoDetail?,
    loading: Boolean,
    error: String?,
    connectionFailed: Boolean,
    isLandscape: Boolean,
    ids: ResolvedVideoIds,
    modifier: Modifier = Modifier,
    infoExpanded: Boolean,
    onToggleInfo: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onOpenTeam: () -> Unit,
    onOpenTeamMember: (VideoStaff) -> Unit,
    onFollowClick: (Long) -> Unit,
    isSelf: (Long) -> Boolean,
    onDownloadClick: () -> Unit,
    actionState: VideoActionUiState,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onOpenCoinPicker: () -> Unit,
    onOpenFavoritePicker: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenComments: () -> Unit
) {
    val actions: @Composable () -> Unit = {
        ActionCapsule(
            stat = detail?.stat,
            state = actionState,
            onToggleLike = onToggleLike,
            onToggleDislike = onToggleDislike,
            onOpenCoinPicker = onOpenCoinPicker,
            onOpenFavoritePicker = onOpenFavoritePicker,
            onOpenShare = onOpenShare,
            onDownloadClick = onDownloadClick
        )
    }
    when {
        loading && detail == null -> VideoDetailInfoSkeleton(modifier = modifier, actionRow = actions)
        connectionFailed -> Spacer(modifier)
        !loading && !error.isNullOrBlank() -> StateMessageCard(error, modifier = modifier, isError = true)
        detail == null -> StateMessageCard("暂无简介信息", modifier = modifier)
        else -> {
            val summaryModifier = if (isLandscape) {
                modifier.verticalScroll(key(ids.aid) { rememberScrollState() })
            } else {
                modifier
            }
            Column(
                modifier = summaryModifier.fillMaxWidth(),
                verticalArrangement = if (isLandscape) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (detail.staffs.isNotEmpty()) {
                        key(ids.aid) {
                            VideoTeamPreview(
                                members = detail.staffs,
                                followingByMid = actionState.followingByMid,
                                followBusyMids = actionState.followBusyMids,
                                isSelf = isSelf,
                                onOpenTeam = onOpenTeam,
                                onOpenMember = onOpenTeamMember,
                                onFollowClick = onFollowClick
                            )
                        }
                    } else {
                        detail.owner?.let { owner ->
                            val spaceRoute = detail.toSpaceRouteOrNull(ids.aid.takeIf { it > 0L })
                            OwnerRow(
                                owner = owner,
                                showStatsSkeleton = loading,
                                onClick = spaceRoute?.let { route ->
                                    { onOpenSpace(route) }
                                },
                                following = actionState.followingByMid[owner.mid] ?: owner.isFollowing,
                                followBusy = owner.mid in actionState.followBusyMids,
                                onFollowClick = if (loading || isSelf(owner.mid)) null else {
                                    { onFollowClick(owner.mid) }
                                }
                            )
                        }
                    }
                    VideoInfoBlock(
                        detail = detail,
                        ids = ids,
                        infoExpanded = infoExpanded,
                        onToggleInfo = onToggleInfo,
                        onOpenComments = onOpenComments,
                        showMetaSkeleton = loading
                    )
                }
                actions()
            }
        }
    }
}

@Composable
private fun OwnerRow(
    owner: VideoOwner,
    modifier: Modifier = Modifier,
    showStatsSkeleton: Boolean = false,
    onClick: (() -> Unit)? = null,
    following: Boolean = false,
    followBusy: Boolean = false,
    onFollowClick: (() -> Unit)? = null
) {
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = onClick)
    } else {
        modifier.fillMaxWidth()
    }
    Row(
        modifier = rowModifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarImage(
            url = owner.face,
            contentDescription = owner.name,
            modifier = Modifier.size(40.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = owner.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OwnerStatText(
                    value = owner.fansText,
                    showSkeleton = showStatsSkeleton,
                    skeletonWidth = 52.dp
                )
                OwnerStatText(
                    value = owner.arcCountText,
                    showSkeleton = showStatsSkeleton,
                    skeletonWidth = 44.dp
                )
            }
        }
        if (onFollowClick != null) {
            FollowButton(
                following = following,
                followedBy = owner.isFollowedBy,
                busy = followBusy,
                onClick = onFollowClick
            )
        }
    }
}

@Composable
internal fun FollowButton(
    following: Boolean,
    followedBy: Boolean,
    busy: Boolean,
    onClick: () -> Unit,
    compactName: String? = null
) {
    val text = when {
        following && followedBy -> "互相关注"
        following -> "已关注"
        followedBy -> "回关"
        else -> "关注"
    }
    val colors = if (following) {
        ButtonDefaults.buttonColors()
    } else {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = if (compactName == null) ButtonDefaults.shape else CircleShape,
        colors = colors,
        contentPadding = if (compactName == null) {
            PaddingValues(horizontal = 14.dp, vertical = 4.dp)
        } else {
            PaddingValues(0.dp)
        },
        modifier = if (compactName == null) Modifier.height(32.dp) else Modifier.size(32.dp)
    ) {
        if (compactName == null) {
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        } else {
            Icon(
                imageVector = if (following) MaterialSymbols.OutlinedFilled.Check else MaterialSymbols.Outlined.Add,
                contentDescription = "$text $compactName",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun OwnerStatText(
    value: String?,
    showSkeleton: Boolean,
    skeletonWidth: Dp
) {
    val text = value?.takeIf(String::isNotBlank)
    if (text != null) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else if (showSkeleton) {
        OwnerStatSkeleton(width = skeletonWidth)
    }
}

@Composable
private fun OwnerStatSkeleton(width: Dp) {
    SkeletonBlock(
        modifier = Modifier
            .width(width)
            .height(14.dp),
        shape = MaterialTheme.shapes.extraSmall
    )
}

@Composable
private fun VideoInfoBlock(
    detail: VideoDetail,
    ids: ResolvedVideoIds,
    infoExpanded: Boolean,
    onToggleInfo: () -> Unit,
    onOpenComments: () -> Unit,
    showMetaSkeleton: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onToggleInfo
                )
        ) {
            Box(Modifier.fillMaxWidth()) {
                val expandIconWidth = 32.dp
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (infoExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = expandIconWidth + 8.dp)
                        .copyTextOnLongPress(detail.title, "标题", onTap = onToggleInfo)
                )
                // Let the title text determine the spacing above the metadata.
                Icon(
                    imageVector = if (infoExpanded) {
                        MaterialSymbols.OutlinedFilled.Keyboard_arrow_up
                    } else {
                        MaterialSymbols.OutlinedFilled.Keyboard_arrow_down
                    },
                    contentDescription = if (infoExpanded) "收起视频信息" else "展开视频信息",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .matchParentSize()
                        .wrapContentSize(Alignment.TopEnd)
                        .size(width = expandIconWidth, height = 24.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onToggleInfo)
                        .padding(horizontal = 4.dp)
                )
            }

            VideoMetaRow(
                detail = detail,
                onOpenComments = onOpenComments,
                showSkeleton = showMetaSkeleton,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        AnimatedVisibility(visible = infoExpanded) {
            VideoInfoPanel(
                detail = detail,
                ids = ids,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoMetaRow(
    detail: VideoDetail,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier,
    showSkeleton: Boolean = false
) {
    val stat = detail.stat
    val pubTs = detail.pubTs
    if (stat == null && pubTs == null && !showSkeleton) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MetaItem(
            icon = MaterialSymbols.Outlined.Play_circle,
            text = stat?.view,
            showSkeleton = showSkeleton,
            skeletonWidth = 48.dp
        )
        MetaItem(
            icon = MaterialSymbols.Outlined.Subtitles,
            text = stat?.danmaku,
            showSkeleton = showSkeleton,
            skeletonWidth = 40.dp
        )
        MetaItem(
            icon = MaterialSymbols.Outlined.Chat_bubble,
            text = stat?.reply,
            showSkeleton = showSkeleton,
            skeletonWidth = 40.dp,
            onClick = onOpenComments
        )
        if (pubTs != null) {
            Text(
                text = formatPubTime(pubTs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (showSkeleton) {
            MetaSkeleton(width = 96.dp)
        }
    }
}

@Composable
private fun MetaItem(
    icon: ImageVector,
    text: String?,
    modifier: Modifier = Modifier,
    showSkeleton: Boolean = false,
    skeletonWidth: Dp = 40.dp,
    onClick: (() -> Unit)? = null
) {
    val value = text?.takeIf(String::isNotBlank)
    if (value == null) {
        if (showSkeleton) {
            MetaSkeleton(width = skeletonWidth)
        }
        return
    }
    val itemModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else {
        modifier
    }
    Row(
        modifier = itemModifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetaSkeleton(width: Dp) {
    SkeletonBlock(
        modifier = Modifier
            .width(width)
            .height(14.dp),
        shape = MaterialTheme.shapes.extraSmall
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoInfoPanel(
    detail: VideoDetail,
    ids: ResolvedVideoIds,
    modifier: Modifier = Modifier
) {
    val aid = ids.aid.takeIf { it > 0L }
    val bvid = ids.bvid?.takeIf(String::isNotBlank)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (aid != null || bvid != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                aid?.let { InfoId(value = "AV$it", copyLabel = "AV号") }
                bvid?.let { InfoId(value = it, copyLabel = "BV号") }
            }
        }

        if (detail.desc.isNotBlank()) {
            InfoSection(label = "简介") {
                Text(
                    text = detail.desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.copyTextOnLongPress(detail.desc, "简介")
                )
            }
        }

        if (detail.tags.isNotEmpty()) {
            InfoSection(label = "标签") {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detail.tags.forEach { tag -> SoftChip(tag) }
                }
            }
        }
    }
}

@Composable
private fun InfoId(
    value: String,
    copyLabel: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.copyTextOnLongPress(value, copyLabel)
    )
}

@Composable
private fun InfoSection(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        content()
    }
}

@Composable
private fun ActionCapsule(
    stat: VideoStat?,
    state: VideoActionUiState,
    modifier: Modifier = Modifier,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onOpenCoinPicker: () -> Unit,
    onOpenFavoritePicker: () -> Unit,
    onOpenShare: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val coinSelected = state.userCoinCount > 0
    val coinContainerColor = if (coinSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val coinGlyphColor = if (coinSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.surface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ActionItem(
            icon = if (state.liked) MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledThumbup else MaterialSymbols.Outlined.SymbolOutlinedThumbup,
            label = "点赞",
            value = stat?.like?.let { compactCount(adjustedCount(it, state.likeCountDelta)) } ?: "-",
            selected = state.liked,
            enabled = state.initialized && !state.likeBusy,
            onClick = onToggleLike,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = if (state.disliked) MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledThumbdown else MaterialSymbols.Outlined.SymbolOutlinedThumbdown,
            label = "不喜欢",
            selected = state.disliked,
            enabled = state.initialized && !state.dislikeBusy,
            onClick = onToggleDislike,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = AppIcons.Coin,
            label = "投币",
            value = stat?.coin?.let { compactCount(adjustedCount(it, state.coinCountDelta)) } ?: "-",
            selected = coinSelected,
            enabled = state.initialized && !state.coinBusy,
            iconContainerColor = coinContainerColor,
            iconGlyphColor = coinGlyphColor,
            onClick = onOpenCoinPicker,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = if (state.favorited) MaterialSymbols.OutlinedFilled.SymbolOutlinedFilledStar else MaterialSymbols.Outlined.SymbolOutlinedStar,
            label = "收藏",
            value = stat?.fav?.let { compactCount(adjustedCount(it, state.favoriteCountDelta)) } ?: "-",
            selected = state.favorited,
            enabled = state.initialized && !state.favoriteLoading && !state.favoriteSaving,
            onClick = onOpenFavoritePicker,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = MaterialSymbols.Outlined.Open_in_new,
            label = "分享",
            value = stat?.share?.let(::compactCount) ?: "-",
            onClick = onOpenShare,
            modifier = Modifier.weight(1f)
        )
        ActionItem(
            icon = MaterialSymbols.Outlined.Download,
            label = "下载",
            onClick = onDownloadClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SoftChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActionItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    iconContainerColor: Color? = null,
    iconGlyphColor: Color = Color.White,
    onClick: () -> Unit
) {
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "actionItemContent"
    )
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        color = Color.Transparent,
        contentColor = contentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ActionItemIcon(
                icon = icon,
                label = label,
                containerColor = iconContainerColor,
                glyphColor = iconGlyphColor
            )
            Text(
                text = value ?: label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ActionItemIcon(
    icon: ImageVector,
    label: String,
    containerColor: Color?,
    glyphColor: Color
) {
    if (containerColor == null) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        return
    }
    Icon(
        imageVector = icon,
        contentDescription = label,
        tint = glyphColor,
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(containerColor)
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CoinSheet(
    selectedAmount: Int,
    busy: Boolean,
    onSelectAmount: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!busy) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = "投币支持一下",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (amount in 1..2) {
                    CoinAmountOption(
                        amount = amount,
                        selected = selectedAmount == amount,
                        enabled = !busy,
                        onClick = { onSelectAmount(amount) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !busy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认投币")
                }
            }
        }
    }
}

@Composable
private fun CoinAmountOption(
    amount: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .height(84.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.small,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = amount.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = "枚硬币",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteFolderSheet(
    folders: List<VideoFavoriteFolder>,
    selectedFolderIds: Set<Long>,
    saving: Boolean,
    onSelectFolder: (Long, Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                text = "选择收藏夹",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                style = MaterialTheme.typography.titleLarge
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 88.dp, max = 420.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = folders,
                    key = { it.id }
                ) { folder ->
                    val selected = folder.id in selectedFolderIds
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = !saving,
                                interactionSource = interactionSource,
                                indication = null
                            ) { onSelectFolder(folder.id, !selected) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folder.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${folder.mediaCount} 个内容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FavoriteSelectionIndicator(selected = selected)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !saving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }
                Button(
                    onClick = onConfirm,
                    enabled = !saving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("完成")
                }
            }
        }
    }
}

@Composable
private fun FavoriteSelectionIndicator(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .then(
                if (selected) {
                    Modifier.background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    )
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = MaterialSymbols.OutlinedFilled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

private enum class ShareChannel(
    val label: String,
    val glyph: String,
    val color: androidx.compose.ui.graphics.Color,
    val packageName: String? = null,
    val componentClassName: String? = null
) {
    WECHAT("微信", "微", androidx.compose.ui.graphics.Color(0xFF07C160), "com.tencent.mm"),
    WECHAT_MOMENTS(
        "朋友圈",
        "圈",
        androidx.compose.ui.graphics.Color(0xFF34BFA3),
        "com.tencent.mm",
        "com.tencent.mm.ui.tools.ShareToTimeLineUI"
    ),
    QQ("QQ", "Q", androidx.compose.ui.graphics.Color(0xFF12B7F5), "com.tencent.mobileqq"),
    WEIBO("微博", "微", androidx.compose.ui.graphics.Color(0xFFE6162D), "com.sina.weibo"),
    COPY_LINK("复制链接", "链", androidx.compose.ui.graphics.Color(0xFFE9E9EE))
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ShareSheet(
    title: String,
    url: String,
    onDismiss: () -> Unit,
    onShare: (ShareChannel) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Text(
                text = "分享",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareChannel.entries.forEach { channel ->
                    ShareChannelItem(
                        channel = channel,
                        onClick = { onShare(channel) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareChannelItem(
    channel: ShareChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = channel.color
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = channel.glyph,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (channel == ShareChannel.COPY_LINK) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        androidx.compose.ui.graphics.Color.White
                    }
                )
            }
        }
        Text(
            text = channel.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun buildVideoShareUrl(ids: ResolvedVideoIds): String? {
    ids.bvid?.takeIf(String::isNotBlank)?.let { return "https://www.bilibili.com/video/$it" }
    return ids.aid.takeIf { it > 0L }?.let { "https://www.bilibili.com/video/av$it" }
}

private fun formatShareTitle(title: String): String {
    return if (title.startsWith("【") && title.endsWith("】")) {
        title
    } else {
        "【$title】"
    }
}

private fun launchShareTarget(
    context: Context,
    channel: ShareChannel,
    title: String,
    url: String
): Boolean {
    val packageName = channel.packageName ?: return false

    fun buildIntent(action: String, componentClassName: String? = null): Intent {
        return Intent(action).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
            if (componentClassName != null) {
                component = ComponentName(packageName, componentClassName)
            } else {
                setPackage(packageName)
            }
        }
    }

    val intents = listOf(buildIntent(Intent.ACTION_SEND, channel.componentClassName))

    for (intent in intents) {
        try {
            context.startActivity(intent)
            return true
        } catch (_: ActivityNotFoundException) {
            // No compatible share target is installed.
        } catch (_: SecurityException) {
            // The target does not expose its share entry point.
        }
    }
    return false
}

private fun adjustedCount(value: String, delta: Int): String {
    if (delta == 0) return value
    return value.toLongOrNull()
        ?.let { (it + delta).coerceAtLeast(0L).toString() }
        ?: value
}

@Composable
private fun SeasonEntryCard(
    season: VideoSeason,
    curCid: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (title, progressText) = remember(season, curCid) { seasonEntryText(season, curCid) }

    DetailEntryRow(
        title = title,
        progressText = progressText,
        modifier = modifier,
        onClick = onClick
    )
}

@Composable
private fun DetailEntryRow(
    title: String,
    progressText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            AnimatedLiveBadge(
                tint = MaterialTheme.colorScheme.primary,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 12.dp)
                    .size(13.dp)
            )
            Text(
                text = progressText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 6.dp)
            )
            Icon(
                imageVector = MaterialSymbols.Outlined.Chevron_right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .size(22.dp)
            )
        }
    }
}

@Composable
private fun SeasonSheetContent(
    season: VideoSeason,
    ids: ResolvedVideoIds,
    currentPubTs: Long?,
    loadPublishTimes: suspend (Long, Long) -> Map<Long, Long>,
    onAddToWatchLater: (Long) -> Unit,
    onOpenEpisode: (VideoTarget) -> Unit
) {
    val selectedIndex = season.sheetItemIndex(ids)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex ?: 0)
    var publishTimes by remember(season.id) { mutableStateOf(emptyMap<Long, Long>()) }
    LaunchedEffect(ids.aid, season.id) {
        if (ids.aid > 0L && season.id > 0L && publishTimes.isEmpty()) {
            publishTimes = loadPublishTimes(ids.aid, season.id)
        }
    }

    DetailSelectionList(
        state = listState,
        selectedIndex = selectedIndex
    ) { cardWidth ->
        item(
            key = "season_title",
            contentType = "title"
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = season.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    style = MaterialTheme.typography.titleLarge
                )
                season.subTitle?.takeIf(String::isNotBlank)?.let { subTitle ->
                    Text(
                        text = subTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        season.sections.forEachIndexed { secIdx, sec ->
            item(
                key = "sec_$secIdx",
                contentType = "section"
            ) {
                Column(Modifier.padding(horizontal = 10.dp)) {
                    if (secIdx > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                    if (sec.title.isNotBlank()) {
                        Text(
                            text = sec.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            items(
                items = sec.eps,
                key = { "ep_${it.cid}_${it.title}" },
                contentType = { "episode" }
            ) { ep ->
                val selected = ep.isCurrent(ids)
                SeasonEpisodeRow(
                    ep = ep,
                    cardWidth = cardWidth,
                    selected = selected,
                    pubTs = ep.pubTs ?: publishTimes[ep.cid] ?: currentPubTs.takeIf { selected },
                    onAddToWatchLater = onAddToWatchLater,
                    onClick = {
                        if (!selected) {
                            onOpenEpisode(ep.target)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PageSheetContent(
    pages: List<VideoPagePart>,
    curCid: Long?,
    aid: Long,
    pubTs: Long?,
    loadCovers: suspend (Long) -> Map<Long, String>,
    onSwitchPage: (Long) -> Unit
) {
    val selectedIndex = remember(pages, curCid) {
        pages.indexOfFirst { it.cid == curCid }.takeIf { it >= 0 }?.plus(1)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex ?: 0)
    // Parts belong to one video; the API provides its publication time, not a date per part.
    val publishTimeText = remember(pubTs) { pubTs?.takeIf { it > 0L }?.let(::formatPubTime) }
    var covers by remember(aid) { mutableStateOf(emptyMap<Long, String>()) }
    LaunchedEffect(aid) {
        covers = loadCovers(aid)
    }

    DetailSelectionList(
        state = listState,
        selectedIndex = selectedIndex
    ) { cardWidth ->
        item(
            key = "page_title",
            contentType = "title"
        ) {
            Text(
                text = "分P列表",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            )
        }

        itemsIndexed(
            items = pages,
            key = { _, page -> "page_${page.cid}" },
            contentType = { _, _ -> "page" }
        ) { index, page ->
            val title = remember(index, page.part) { buildPageTitle(index, page.part) }
            val durationText = remember(page.durationSec) {
                page.durationSec.takeIf { it > 0L }?.let { formatDuration(it * 1000) }
            }
            PageSheetRow(
                title = title,
                durationText = durationText,
                cardWidth = cardWidth,
                publishTimeText = publishTimeText,
                coverUrl = covers[page.cid],
                selected = page.cid == curCid,
                onClick = {
                    if (page.cid != curCid) {
                        onSwitchPage(page.cid)
                    }
                }
            )
        }
    }
}

@Composable
private fun SelectionVideoCard(
    title: String,
    coverUrl: String?,
    durationText: String?,
    cardWidth: Dp,
    selected: Boolean,
    onClick: () -> Unit,
    coverPlaceholder: (@Composable BoxScope.() -> Unit)? = null,
    metadata: @Composable (Dp) -> Unit
) {
    val titleColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val compact = isPhoneLandscape()
    val annotatedTitle = remember(title, titleColor, selected) {
        buildAnnotatedString {
            if (selected) {
                appendInlineContent(SELECTION_PLAYING_TITLE_ICON, "当前播放")
                append(' ')
            }
            withStyle(SpanStyle(color = titleColor)) { append(title) }
        }
    }
    val titleInlineContent = remember(selected, titleColor) {
        if (selected) {
            mapOf(
                SELECTION_PLAYING_TITLE_ICON to InlineTextContent(
                    Placeholder(1.em, 1.em, PlaceholderVerticalAlign.TextCenter)
                ) {
                    AnimatedLiveBadge(
                        modifier = Modifier.fillMaxSize(),
                        tint = titleColor,
                        contentDescription = null
                    )
                }
            )
        } else emptyMap()
    }
    HorizontalVideoCard(
        title = annotatedTitle,
        coverUrl = coverUrl,
        durationText = durationText,
        onClick = if (selected) null else onClick,
        cardWidth = cardWidth,
        coverSize = if (compact) null else DpSize(160.dp, 90.dp),
        compact = compact,
        coverPlaceholder = coverPlaceholder,
        titleInlineContent = titleInlineContent,
        isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600,
        metadata = metadata
    )
}

@Composable
private fun SeasonEpisodeRow(
    ep: VideoSeasonEpisode,
    cardWidth: Dp,
    selected: Boolean,
    pubTs: Long?,
    onAddToWatchLater: (Long) -> Unit,
    onClick: () -> Unit
) {
    val publishTimeText = remember(pubTs, ep.publishTimeText) {
        pubTs?.let(::formatPubTime) ?: ep.publishTimeText?.takeIf(String::isNotBlank)
    }
    val viewText = remember(ep.viewText) { ep.viewText?.takeIf(String::isNotBlank)?.let(::compactCount) }
    val danmakuText = remember(ep.danmakuText) { ep.danmakuText?.takeIf(String::isNotBlank)?.let(::compactCount) }
    val aid = when (val target = ep.target) {
        is VideoTarget.Ugc -> target.aid
        is VideoTarget.Pgc -> target.aid
        is VideoTarget.Pugv -> target.aid
    }

    SelectionVideoCard(
        title = ep.title,
        coverUrl = ep.cover,
        durationText = ep.durationText,
        cardWidth = cardWidth,
        selected = selected,
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            publishTimeText?.let { publishTime ->
                Text(
                    text = publishTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            VideoCardStatistics(
                viewText = viewText,
                danmakuText = danmakuText,
                onAddToWatchLater = if (aid > 0L) ({ onAddToWatchLater(aid) }) else null
            )
        }
    }
}

@Composable
private fun PageSheetRow(
    title: String,
    durationText: String?,
    cardWidth: Dp,
    publishTimeText: String?,
    coverUrl: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    SelectionVideoCard(
        title = title,
        coverUrl = coverUrl,
        durationText = durationText,
        cardWidth = cardWidth,
        selected = selected,
        onClick = onClick,
        coverPlaceholder = {
            Icon(
                imageVector = MaterialSymbols.Outlined.Movie,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    ) {
        publishTimeText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RelateRow(
    relate: VideoRelate,
    cardWidth: Dp,
    onOpenVideo: (VideoTarget) -> Unit,
    onAddToWatchLater: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewText = relate.viewText?.takeIf(String::isNotBlank)?.let(::compactCount)
    val danmakuText = relate.danmakuText?.takeIf(String::isNotBlank)?.let(::compactCount)
    val author = relate.author?.takeIf(String::isNotBlank)
    val reason = relate.reason?.takeIf(String::isNotBlank)
    HorizontalVideoCard(
        title = relate.title,
        coverUrl = relate.cover,
        durationText = relate.durationText,
        onClick = { onOpenVideo(relate.target) },
        modifier = modifier,
        cardWidth = cardWidth,
        compact = isPhoneLandscape(),
        isTablet = LocalConfiguration.current.smallestScreenWidthDp >= 600
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (reason != null && author != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    RelateReasonBadge(reason, Modifier.layout { measurable, constraints ->
                        val maxBadgeWidth = (constraints.maxWidth * 0.4f).toInt()
                        val badge = measurable.measure(constraints.copy(maxWidth = maxBadgeWidth))
                        layout(badge.width, badge.height) { badge.placeRelative(0, 0) }
                    })
                    RelateAuthor(author, showBadge = false, modifier = Modifier.weight(1f))
                }
            } else if (reason != null) {
                RelateReasonBadge(reason)
            } else if (author != null) {
                RelateAuthor(author, showBadge = true, modifier = Modifier.fillMaxWidth())
            }
            VideoCardStatistics(
                viewText = viewText,
                danmakuText = danmakuText,
                onAddToWatchLater = { onAddToWatchLater(relate.target.aid) }
            )
        }
    }
}

@Composable
private fun RelateReasonBadge(reason: String, modifier: Modifier = Modifier) {
    Text(
        text = reason,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraSmall)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RelateAuthor(author: String, showBadge: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (showBadge) UploaderBadge()
        Text(
            text = author,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VideoCardStatistics(
    viewText: String?,
    danmakuText: String?,
    onAddToWatchLater: (() -> Unit)?
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (viewText != null) {
            Icon(
                imageVector = MaterialSymbols.Outlined.Play_circle,
                contentDescription = "播放量",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = viewText,
                modifier = Modifier.padding(start = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (danmakuText != null) {
            Icon(
                imageVector = MaterialSymbols.Outlined.Subtitles,
                contentDescription = "弹幕数",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(15.dp)
            )
            Text(
                text = danmakuText,
                modifier = Modifier.padding(start = 3.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (onAddToWatchLater != null) {
            WatchLaterMoreMenu(onAddToWatchLater)
        }
    }
}

@Composable
internal fun QualityOptionItem(
    option: QualityOption,
    isSelected: Boolean,
    onClick: (() -> Unit)? = null
) {
    val rowMod = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    } else {
        Modifier.fillMaxWidth()
    }
    Row(
        modifier = rowMod.padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (option.needVip) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "大会员",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            option.limit?.message?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = MaterialSymbols.OutlinedFilled.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun seasonEntryText(
    season: VideoSeason,
    curCid: Long?
): Pair<String, String> {
    var total = 0
    var curIdx = -1
    season.sections.forEach { section ->
        if (curIdx < 0 && curCid != null) {
            val index = section.eps.indexOfFirst { it.cid == curCid }
            if (index >= 0) curIdx = total + index
        }
        total += section.eps.size
    }
    return "合集 · ${season.title}" to if (curIdx >= 0) {
        "${curIdx + 1}/$total"
    } else {
        "共 $total 个"
    }
}

private fun buildPageTitle(index: Int, part: String): String {
    return "P${index + 1} $part"
}

private fun formatPubTime(ts: Long): String {
    return DateFormat.format("yyyy-MM-dd HH:mm", ts * 1000).toString()
}

private fun VideoDetail.toSpaceRouteOrNull(fromViewAid: Long?): SpaceRoute? {
    val owner = owner ?: return null
    if (owner.mid <= 0L && owner.name.isBlank()) return null
    return SpaceRoute(
        mid = owner.mid,
        name = owner.name.takeIf(String::isNotBlank),
        from = SpaceRouteTool.FROM_DEFAULT,
        fromViewAid = fromViewAid
    )
}


private suspend fun LazyListState.animateContinuouslyToTop() {
    if (!canScrollBackward) return

    val durationScale = coroutineContext[MotionDurationScale]
    scroll {
        val lazyScope = LazyLayoutScrollScope(this@animateContinuouslyToTop, this)
        val viewportPx = layoutInfo.viewportSize.height.coerceAtLeast(1).toFloat()
        val estimatedDistancePx = abs(lazyScope.calculateDistanceTo(0).toFloat())
        if ((durationScale?.scaleFactor ?: 1f) <= 0f ||
            estimatedDistancePx >= viewportPx * COMMENT_SCROLL_TO_TOP_INSTANT_THRESHOLD_VIEWPORTS
        ) {
            lazyScope.snapToItem(0)
            return@scroll
        }

        // The estimate sets the speed, not an intermediate destination for tall comments.
        val durationSeconds = (180f + estimatedDistancePx / viewportPx * 35f)
            .coerceAtMost(700f) / 1000f
        val pixelsPerSecond = (estimatedDistancePx / durationSeconds)
            .coerceIn(80f, (viewportPx * 18f).coerceAtLeast(80f))
        var previousFrame = withFrameNanos { it }

        while (this@animateContinuouslyToTop.canScrollBackward) {
            val frame = withFrameNanos { it }
            val elapsedNanos = (frame - previousFrame).coerceIn(0L, 32_000_000L)
            previousFrame = frame
            val scale = durationScale?.scaleFactor ?: 1f
            if (scale <= 0f) {
                lazyScope.snapToItem(0)
                return@scroll
            }
            if (elapsedNanos == 0L) continue

            val consumed = lazyScope.scrollBy(-pixelsPerSecond * elapsedNanos / 1_000_000_000f / scale)
            if (consumed == 0f) break
        }
        if (this@animateContinuouslyToTop.canScrollBackward &&
            firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset <= 2
        ) {
            lazyScope.snapToItem(0)
        }
    }
}

private const val COMMENT_SCROLL_TO_TOP_INSTANT_THRESHOLD_VIEWPORTS = 8f
private const val DETAIL_RELATE_SKELETON_ROWS = 4
private const val SELECTION_PLAYING_TITLE_ICON = "selection_playing"
private sealed interface DetailSheet {
    data class Season(
        val season: VideoSeason,
        val ids: ResolvedVideoIds
    ) : DetailSheet

    data class Page(
        val pages: List<VideoPagePart>,
        val curCid: Long?
    ) : DetailSheet
}
