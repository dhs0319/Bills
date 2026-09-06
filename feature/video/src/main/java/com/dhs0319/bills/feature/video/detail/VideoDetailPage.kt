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
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.BiliImageVariant
import com.dhs0319.bills.core.designsystem.component.StateMessageCard
import com.dhs0319.bills.core.designsystem.component.VideoDetailInfoSkeleton
import com.dhs0319.bills.core.designsystem.component.VideoRelateCardSkeleton
import com.dhs0319.bills.core.designsystem.component.copyTextOnLongPress
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
import com.dhs0319.bills.core.model.VideoStat
import com.dhs0319.bills.feature.comment.CommentPanel
import com.dhs0319.bills.feature.video.formatDuration
import com.dhs0319.bills.core.video.VideoFavoriteFolder
import com.dhs0319.bills.feature.video.action.VideoActionUiState
import kotlinx.coroutines.launch
@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun VideoDetailPage(
    modifier: Modifier = Modifier,
    detail: VideoDetail?,
    ids: ResolvedVideoIds,
    detailLoading: Boolean,
    detailError: String?,
    actionState: VideoActionUiState,
    commentSubject: CommentSubject?,
    contentHorizontalPad: Dp,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onDownloadClick: () -> Unit,
    onToggleLike: () -> Unit,
    isLoggedIn: () -> Boolean,
    onSubmitCoins: (Int, () -> Unit) -> Unit,
    onLoadFavoriteFolders: ((List<VideoFavoriteFolder>) -> Unit) -> Unit,
    onSaveFavoriteFolders: (Set<Long>, Set<Long>, () -> Unit) -> Unit,
    onOpenEpisode: (VideoTarget) -> Unit,
    onSwitchPage: (Long) -> Unit
) {
    val aidKey = ids.aid.takeIf { it > 0L }
    val curCid = ids.cid.takeIf { it > 0L }
    var descOn by rememberSaveable(aidKey) { mutableStateOf(false) }
    var tagOn by rememberSaveable(aidKey) { mutableStateOf(false) }
    var sheet by remember(aidKey) { mutableStateOf<DetailSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val detailListState = remember(aidKey) { LazyListState() }
    val commentListState = remember(aidKey) { LazyListState() }
    val commentThreadListState = remember(aidKey) { LazyListState() }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    var coinSheetVisible by remember(aidKey) { mutableStateOf(false) }
    var selectedCoinAmount by remember(aidKey) { mutableStateOf(1) }
    var favoriteSheetVisible by remember(aidKey) { mutableStateOf(false) }
    var favoriteFolders by remember(aidKey) { mutableStateOf(emptyList<VideoFavoriteFolder>()) }
    var originalFavoriteFolderIds by remember(aidKey) { mutableStateOf(emptySet<Long>()) }
    var selectedFavoriteFolderIds by remember(aidKey) { mutableStateOf(emptySet<Long>()) }
    var shareSheetVisible by remember(aidKey) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = remember(context) {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }
    val shareUrl = remember(detail, ids) { detail?.let { buildVideoShareUrl(ids) } }

    LaunchedEffect(aidKey) {
        pagerState.scrollToPage(0)
    }

    fun closeSheet(afterClose: (() -> Unit)? = null) {
        scope.launch {
            if (sheetState.isVisible) {
                sheetState.hide()
            }
            sheet = null
            afterClose?.invoke()
        }
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 0,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> DetailPageContent(
                modifier = Modifier.fillMaxSize(),
                detail = detail,
                ids = ids,
                detailLoading = detailLoading,
                detailError = detailError,
                actionState = actionState,
                horizontalPad = contentHorizontalPad,
                infoListState = detailListState,
                descOn = descOn,
                tagOn = tagOn,
                onToggleDesc = { descOn = !descOn },
                onToggleTag = { tagOn = !tagOn },
                onSeasonClick = { sheet = detail?.season?.let { DetailSheet.Season(it, curCid) } },
                onPageClick = {
                    sheet = detail?.pages
                        ?.takeIf { it.size > 1 }
                        ?.let { DetailSheet.Page(it, curCid) }
                },
                onOpenComments = {
                    scope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                onOpenVideo = onOpenVideo,
                onOpenSpace = onOpenSpace,
                onDownloadClick = onDownloadClick,
                onToggleLike = onToggleLike,
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

            else -> {
                CommentPanel(
                    subject = commentSubject,
                    isActive = pagerState.currentPage == page,
                    onOpenSpace = onOpenSpace,
                    modifier = Modifier.fillMaxSize(),
                    listState = commentListState,
                    threadListState = commentThreadListState,
                    contentPadding = PaddingValues(
                        start = contentHorizontalPad,
                        top = 12.dp,
                        end = contentHorizontalPad,
                        bottom = 20.dp
                    )
                )
            }
        }
    }

    sheet?.let { activeSheet ->
        ModalBottomSheet(
            onDismissRequest = { closeSheet() },
            sheetState = sheetState
        ) {
            when (activeSheet) {
                is DetailSheet.Season -> {
                    SeasonSheetContent(
                        season = activeSheet.season,
                        curCid = activeSheet.curCid,
                        onOpenEpisode = { route ->
                            closeSheet {
                                onOpenEpisode(route)
                            }
                        }
                    )
                }

                is DetailSheet.Page -> {
                    PageSheetContent(
                        pages = activeSheet.pages,
                        curCid = activeSheet.curCid,
                        onSwitchPage = { cid ->
                            closeSheet {
                                onSwitchPage(cid)
                            }
                        }
                    )
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
}

@Composable
private fun DetailPageContent(
    modifier: Modifier,
    detail: VideoDetail?,
    ids: ResolvedVideoIds,
    detailLoading: Boolean,
    detailError: String?,
    actionState: VideoActionUiState,
    horizontalPad: Dp,
    infoListState: LazyListState,
    descOn: Boolean,
    tagOn: Boolean,
    onToggleDesc: () -> Unit,
    onToggleTag: () -> Unit,
    onSeasonClick: () -> Unit,
    onPageClick: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onDownloadClick: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenCoinPicker: () -> Unit,
    onOpenFavoritePicker: () -> Unit,
    onOpenShare: () -> Unit
) {
    val itemMod = remember(horizontalPad) {
        if (horizontalPad > 0.dp) Modifier.padding(horizontal = horizontalPad) else Modifier
    }
    val infoTopPad = remember(horizontalPad) {
        if (horizontalPad > 0.dp) 16.dp else 0.dp
    }

    LazyColumn(
        state = infoListState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = infoTopPad, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        detailItems(
            detail = detail,
            ids = ids,
            detailLoading = detailLoading,
            detailError = detailError,
            actionState = actionState,
            itemMod = itemMod,
            descOn = descOn,
            tagOn = tagOn,
            onToggleDesc = onToggleDesc,
            onToggleTag = onToggleTag,
            onSeasonClick = onSeasonClick,
            onPageClick = onPageClick,
            onOpenVideo = onOpenVideo,
            onOpenSpace = onOpenSpace,
            onDownloadClick = onDownloadClick,
            onToggleLike = onToggleLike,
            onOpenCoinPicker = onOpenCoinPicker,
            onOpenFavoritePicker = onOpenFavoritePicker,
            onOpenShare = onOpenShare,
            onOpenComments = onOpenComments
        )
    }
}

private fun LazyListScope.detailItems(
    detail: VideoDetail?,
    ids: ResolvedVideoIds,
    detailLoading: Boolean,
    detailError: String?,
    actionState: VideoActionUiState,
    itemMod: Modifier,
    descOn: Boolean,
    tagOn: Boolean,
    onToggleDesc: () -> Unit,
    onToggleTag: () -> Unit,
    onSeasonClick: () -> Unit,
    onPageClick: () -> Unit,
    onOpenVideo: (VideoTarget) -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onDownloadClick: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenCoinPicker: () -> Unit,
    onOpenFavoritePicker: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenComments: () -> Unit
) {
    val curCid = ids.cid.takeIf { it > 0L }
    when {
        detailLoading -> {
            item(
                key = "detail_loading_summary",
                contentType = "skeleton"
            ) {
                VideoDetailInfoSkeleton(modifier = itemMod)
            }
            items(
                count = DETAIL_RELATE_SKELETON_COUNT,
                key = { index -> "detail_loading_relate_$index" },
                contentType = { "skeleton" }
            ) {
                VideoRelateCardSkeleton(modifier = itemMod)
            }
        }

        !detailError.isNullOrBlank() -> {
            item(
                key = "detail_error",
                contentType = "state"
            ) {
                StateMessageCard(
                    text = detailError,
                    modifier = itemMod,
                    isError = true
                )
            }
        }

        detail != null -> {
            item(
                key = "summary",
                contentType = "summary"
            ) {
                VideoSummarySection(
                    detail = detail,
                    ids = ids,
                    descOn = descOn,
                    tagOn = tagOn,
                    onToggleDesc = onToggleDesc,
                    onToggleTag = onToggleTag,
                    onOpenSpace = onOpenSpace,
                    onDownloadClick = onDownloadClick,
                    actionState = actionState,
                    onToggleLike = onToggleLike,
                    onOpenCoinPicker = onOpenCoinPicker,
                    onOpenFavoritePicker = onOpenFavoritePicker,
                    onOpenShare = onOpenShare,
                    onOpenComments = onOpenComments,
                    modifier = itemMod
                )
            }

            detail.season?.let { season ->
                item(
                    key = "season_entry",
                    contentType = "season_entry"
                ) {
                    SeasonEntryCard(
                        season = season,
                        curCid = curCid,
                        onClick = onSeasonClick,
                        modifier = itemMod
                    )
                }
            }

            if (detail.pages.size > 1) {
                item(
                    key = "page_entry",
                    contentType = "page_entry"
                ) {
                    PageEntryCard(
                        pages = detail.pages,
                        curCid = curCid,
                        onClick = onPageClick,
                        modifier = itemMod
                    )
                }
            }

            if (detail.relates.isNotEmpty()) {
                items(
                    items = detail.relates,
                    key = { "${it.target.aid}_${it.target.cid}" },
                    contentType = { "relate" }
                ) { relate ->
                    RelateRow(
                        relate = relate,
                        onOpenVideo = onOpenVideo,
                        modifier = itemMod
                    )
                }
            }
        }

        else -> {
            item(
                key = "detail_empty",
                contentType = "state"
            ) {
                StateMessageCard(
                    text = "暂无简介信息",
                    modifier = itemMod
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoSummarySection(
    detail: VideoDetail,
    ids: ResolvedVideoIds,
    modifier: Modifier = Modifier,
    descOn: Boolean,
    tagOn: Boolean,
    onToggleDesc: () -> Unit,
    onToggleTag: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onDownloadClick: () -> Unit,
    actionState: VideoActionUiState,
    onToggleLike: () -> Unit,
    onOpenCoinPicker: () -> Unit,
    onOpenFavoritePicker: () -> Unit,
    onOpenShare: () -> Unit,
    onOpenComments: () -> Unit
) {
    val spaceRoute = detail.toSpaceRouteOrNull(ids.aid.takeIf { it > 0L })
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        detail.owner?.let { owner ->
            OwnerCapsule(
                owner = owner,
                onClick = spaceRoute?.let { route ->
                    { onOpenSpace(route) }
                }
            )
        }
        InfoCapsule(
            detail = detail,
            ids = ids,
            descOn = descOn,
            tagOn = tagOn,
            onToggleDesc = onToggleDesc,
            onToggleTag = onToggleTag,
            onOpenComments = onOpenComments
        )
        ActionCapsule(
            stat = detail.stat,
            state = actionState,
            onToggleLike = onToggleLike,
            onOpenCoinPicker = onOpenCoinPicker,
            onOpenFavoritePicker = onOpenFavoritePicker,
            onOpenShare = onOpenShare,
            onDownloadClick = onDownloadClick
        )
    }
}

@Composable
private fun OwnerCapsule(
    owner: VideoOwner,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    CapsuleCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            owner.face?.takeIf(String::isNotBlank)?.let { face ->
                BiliAsyncImage(
                    url = face,
                    contentDescription = owner.name,
                    modifier = Modifier
                        .width(72.dp)
                        .aspectRatio(1f)
                        .clip(CircleShape),
                    variant = BiliImageVariant.Avatar,
                    contentScale = ContentScale.Fit
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = owner.name,
                    style = MaterialTheme.typography.titleMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    owner.fansText?.takeIf(String::isNotBlank)?.let { fans ->
                        SoftChip(fans)
                    }
                    owner.arcCountText?.takeIf(String::isNotBlank)?.let { arcCount ->
                        SoftChip(arcCount)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InfoCapsule(
    detail: VideoDetail,
    ids: ResolvedVideoIds,
    descOn: Boolean,
    tagOn: Boolean,
    onToggleDesc: () -> Unit,
    onToggleTag: () -> Unit,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    CapsuleCard(modifier = modifier) {
        Text(
            text = detail.title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.copyTextOnLongPress(detail.title, "标题")
        )

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ids.aid.takeIf { it > 0L }?.let { SoftChip("AV$it", onLongPressLabel = "AV号") }
            ids.bvid?.takeIf(String::isNotBlank)?.let { SoftChip(it, onLongPressLabel = "BV号") }
            detail.pubTs?.let { ts ->
                SoftChip(formatPubTime(ts))
            }
            detail.stat?.let { stat ->
                SoftChip("${stat.view} 播放")
                SoftChip("${stat.danmaku} 弹幕")
                SoftChip(
                    text = "${stat.reply} 评论",
                    onClick = onOpenComments
                )
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (detail.desc.isNotBlank()) {
                ToggleChip(
                    text = "简介",
                    expanded = descOn,
                    onClick = onToggleDesc
                )
            }
            if (detail.tags.isNotEmpty()) {
                ToggleChip(
                    text = "标签",
                    expanded = tagOn,
                    onClick = onToggleTag
                )
            }
        }

        if (descOn && detail.desc.isNotBlank()) {
            Text(
                text = detail.desc,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .copyTextOnLongPress(detail.desc, "简介")
            )
        }

        if (tagOn && detail.tags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                detail.tags.forEach { tag ->
                    SoftChip(tag)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActionCapsule(
    stat: VideoStat?,
    state: VideoActionUiState,
    modifier: Modifier = Modifier,
    onToggleLike: () -> Unit,
    onOpenCoinPicker: () -> Unit,
    onOpenFavoritePicker: () -> Unit,
    onOpenShare: () -> Unit,
    onDownloadClick: () -> Unit
) {
    CapsuleCard(modifier = modifier) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stat?.let {
                ActionChip(
                    label = "点赞",
                    value = adjustedCount(it.like, state.likeCountDelta),
                    selected = state.liked,
                    enabled = state.initialized && !state.likeBusy,
                    onClick = onToggleLike
                )
                ActionChip(
                    label = "投币",
                    value = adjustedCount(it.coin, state.coinCountDelta),
                    selected = state.userCoinCount > 0,
                    enabled = state.initialized && !state.coinBusy,
                    onClick = onOpenCoinPicker
                )
                ActionChip(
                    label = "收藏",
                    value = adjustedCount(it.fav, state.favoriteCountDelta),
                    selected = state.favorited,
                    enabled = state.initialized && !state.favoriteLoading && !state.favoriteSaving,
                    onClick = onOpenFavoritePicker
                )
                ActionChip(
                    label = "分享",
                    onClick = onOpenShare
                )
            }
            ActionChip(
                label = "下载",
                onClick = onDownloadClick
            )
        }
    }
}

@Composable
private fun CapsuleCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SoftChip(
    text: String,
    onClick: (() -> Unit)? = null,
    onLongPressLabel: String? = null
) {
    val modifier = when {
        onLongPressLabel != null -> Modifier.copyTextOnLongPress(text, onLongPressLabel)
        onClick != null -> Modifier.clickable(onClick = onClick)
        else -> Modifier
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ToggleChip(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Icon(
                imageVector = if (expanded) {
                    Icons.Default.KeyboardArrowUp
                } else {
                    Icons.Default.KeyboardArrowDown
                },
                contentDescription = if (expanded) "收起" else "展开",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    value: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = tween(durationMillis = 150),
        label = "actionChipContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 150),
        label = "actionChipContent"
    )
    Surface(
        modifier = if (onClick != null) {
            Modifier
                .alpha(if (enabled) 1f else 0.55f)
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        } else {
            Modifier
        },
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minWidth = 76.dp, minHeight = 36.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = if (value == null) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                color = contentColor
            )
            value?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleSmall,
                    color = contentColor
                )
            }
        }
    }
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
                imageVector = Icons.Default.Check,
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
    val (title, subTitle, countText) = seasonEntryText(season, curCid)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "合集列表",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subTitle.isNotBlank()) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PageEntryCard(
    pages: List<VideoPagePart>,
    curCid: Long?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val (title, subTitle, countText) = remember(pages, curCid) {
        pageEntryText(pages, curCid)
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "分P列表",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (subTitle.isNotBlank()) {
                Text(
                    text = subTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = countText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SeasonSheetContent(
    season: VideoSeason,
    curCid: Long?,
    onOpenEpisode: (VideoTarget) -> Unit
) {
    val initIdx = remember(season, curCid) { seasonSheetIndex(season, curCid) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIdx)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(
            key = "season_title",
            contentType = "title"
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = season.title,
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
                if (secIdx > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                if (sec.title.isNotBlank()) {
                    Text(
                        text = sec.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            items(
                items = sec.eps,
                key = { "ep_${it.cid}_${it.title}" },
                contentType = { "episode" }
            ) { ep ->
                SeasonEpisodeRow(
                    ep = ep,
                    selected = ep.cid == curCid,
                    onClick = {
                        if (ep.cid != curCid) {
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
    onSwitchPage: (Long) -> Unit
) {
    val pageSheetUi = remember(pages) { buildPageSheetUi(pages) }
    val initIdx = remember(pages, curCid) { pageSheetIndex(pageSheetUi, curCid) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIdx)

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(
            key = "page_title",
            contentType = "title"
        ) {
            Text(
                text = "分P列表",
                style = MaterialTheme.typography.titleLarge
            )
        }

        items(
            items = pageSheetUi.items,
            key = { page -> "page_${page.cid}" },
            contentType = { "page" }
        ) { page ->
            PageSheetRow(
                title = page.title,
                subTitle = page.subTitle,
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
private fun SeasonEpisodeRow(
    ep: VideoSeasonEpisode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val rowMod = if (selected) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Row(
        modifier = rowMod.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        ep.cover?.takeIf(String::isNotBlank)?.let { cover ->
            BiliAsyncImage(
                url = cover,
                contentDescription = ep.title,
                modifier = Modifier
                    .width(112.dp)
                    .aspectRatio(16f / 10f)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ep.title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    CurBadge()
                }
            }
            ep.subTitle?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun PageSheetRow(
    title: String,
    subTitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val rowMod = if (selected) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    }

    Column(
        modifier = rowMod.padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                CurBadge()
            }
        }
        subTitle?.takeIf(String::isNotBlank)?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun RelateRow(
    relate: VideoRelate,
    onOpenVideo: (VideoTarget) -> Unit,
    modifier: Modifier = Modifier
) {
    val meta = listOfNotNull(
        relate.viewText?.let { "$it 播放" },
        relate.danmakuText?.let { "$it 弹幕" },
        relate.durationText
    ).joinToString(" · ")

    Card(
        onClick = { onOpenVideo(relate.target) },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            BiliAsyncImage(
                url = relate.cover,
                contentDescription = relate.title,
                modifier = Modifier
                    .weight(0.38f)
                    .aspectRatio(16f / 10f)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier.weight(0.62f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = relate.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                relate.author?.let { author ->
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                relate.reason?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                MaterialTheme.shapes.extraSmall
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CurBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Text(
            text = "当前播放",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
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
                imageVector = Icons.Default.Check,
                contentDescription = "已选择",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun seasonEntryText(
    season: VideoSeason,
    curCid: Long?
): Triple<String, String, String> {
    val curEp = season.sections
        .asSequence()
        .flatMap { it.eps.asSequence() }
        .firstOrNull { it.cid == curCid }
    return Triple(
        curEp?.title ?: season.title,
        curEp?.subTitle.orEmpty().ifBlank { season.subTitle.orEmpty() },
        "${season.sections.sumOf { it.eps.size }} 个视频"
    )
}

private fun pageEntryText(
    pages: List<VideoPagePart>,
    curCid: Long?
): Triple<String, String, String> {
    val curIdx = curCid?.let { pages.indexOfFirst { it.cid == curCid } } ?: -1
    val curPage = pages.getOrNull(curIdx)
    return Triple(
        if (curPage != null) buildPageTitle(curIdx, curPage.part) else "查看分P列表",
        curPage?.durationSec?.takeIf { it > 0L }?.let { formatDuration(it * 1000) }.orEmpty(),
        "${pages.size} 个分 P"
    )
}

private fun seasonSheetIndex(season: VideoSeason, curCid: Long?): Int {
    if (curCid == null) return 0
    var secItemIdx = 1
    season.sections.forEach { sec ->
        val epIdx = sec.eps.indexOfFirst { it.cid == curCid }
        if (epIdx >= 0) {
            return secItemIdx + 1 + epIdx
        }
        secItemIdx += 1 + sec.eps.size
    }
    return 0
}

private fun pageSheetIndex(
    pageSheetUi: PageSheetUi,
    curCid: Long?
): Int {
    val pageIdx = curCid?.let { targetCid -> pageSheetUi.indexByCid[targetCid] } ?: -1
    return if (pageIdx >= 0) pageIdx + 1 else 0
}

private fun buildPageSheetUi(pages: List<VideoPagePart>): PageSheetUi {
    val items = ArrayList<PageSheetItem>(pages.size)
    val indexByCid = HashMap<Long, Int>(pages.size)
    pages.forEachIndexed { index, page ->
        items += PageSheetItem(
            cid = page.cid,
            title = buildPageTitle(index, page.part),
            subTitle = page.durationSec.takeIf { it > 0L }?.let { formatDuration(it * 1000) }
        )
        indexByCid[page.cid] = index
    }
    return PageSheetUi(
        items = items,
        indexByCid = indexByCid
    )
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

private const val DETAIL_RELATE_SKELETON_COUNT = 2
private sealed interface DetailSheet {
    data class Season(
        val season: VideoSeason,
        val curCid: Long?
    ) : DetailSheet

    data class Page(
        val pages: List<VideoPagePart>,
        val curCid: Long?
    ) : DetailSheet
}

@Immutable
private data class PageSheetUi(
    val items: List<PageSheetItem>,
    val indexByCid: Map<Long, Int>
)

@Immutable
private data class PageSheetItem(
    val cid: Long,
    val title: String,
    val subTitle: String?
)
