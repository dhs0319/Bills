package com.dhs0319.bills.feature.im.feed

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhs0319.bills.core.model.MsgFeedFilter
import com.dhs0319.bills.core.designsystem.component.SlidingTabRow
import com.dhs0319.bills.core.model.PublishedRecord
import com.dhs0319.bills.core.model.LiveRoute
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.VideoTarget
import com.dhs0319.bills.feature.comment.CommentPanel

private val msgFeedTabs = listOf("全部", "@我")
private val msgFeedTabWidth = 88.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MsgFeedScreen(
    onBack: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit = {},
    onOpenVideoDetail: (VideoTarget) -> Unit = {},
    onOpenDynamicDetail: (String) -> Unit = {},
    onOpenLiveDetail: (LiveRoute) -> Unit = {},
    vm: MsgFeedViewModel = hiltViewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    var detailRecord by remember { mutableStateOf<PublishedRecord?>(null) }

    BackHandler(enabled = detailRecord != null) {
        detailRecord = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("回复与@") },
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
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 600.dp)
                        .fillMaxSize()
                ) {
                    SlidingTabRow(
                        tabs = msgFeedTabs,
                        selectedIndex = state.filterType.index,
                        onSelect = { index ->
                            when (index) {
                                0 -> vm.changeFilter(MsgFeedFilter.ALL)
                                1 -> vm.changeFilter(MsgFeedFilter.AT_ME)
                            }
                        },
                        tabHorizontalAlignment = Alignment.Start,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .width(msgFeedTabWidth * msgFeedTabs.size)
                    )
                    MsgFeedPane(
                        onOpenCommentDetail = { record ->
                            detailRecord = record
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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
