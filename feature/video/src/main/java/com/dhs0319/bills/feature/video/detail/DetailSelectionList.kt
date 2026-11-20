package com.dhs0319.bills.feature.video.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.My_location
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun DetailSelectionList(
    state: LazyListState,
    selectedIndex: Int?,
    content: LazyListScope.(cardWidth: Dp) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize().navigationBarsPadding()) {
        // Measure once for the list instead of subcomposing every fixed-size cover card.
        val cardWidth = (maxWidth - 12.dp).coerceAtLeast(0.dp)
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            // Leave room to scroll the last card clear of the floating button.
            contentPadding = PaddingValues(start = 6.dp, top = 12.dp, end = 6.dp, bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = { content(cardWidth) }
        )
        LocateSelectionButton(
            state = state,
            selectedIndex = selectedIndex,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
    }
}

@Composable
private fun LocateSelectionButton(
    state: LazyListState,
    selectedIndex: Int?,
    modifier: Modifier = Modifier
) {
    val showButton by remember(state, selectedIndex) {
        derivedStateOf {
            val layout = state.layoutInfo
            selectedIndex != null && selectedIndex in 0 until layout.totalItemsCount &&
                layout.visibleItemsInfo.isNotEmpty() &&
                layout.visibleItemsInfo.none { item ->
                    item.index == selectedIndex &&
                        item.offset < layout.viewportEndOffset &&
                        item.offset + item.size > layout.viewportStartOffset
                }
        }
    }
    // Keep the scope outside AnimatedVisibility so hiding the button does not cancel scrolling.
    val scope = rememberCoroutineScope()
    AnimatedVisibility(
        visible = showButton,
        modifier = modifier,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120))
    ) {
        SmallFloatingActionButton(
            onClick = {
                val index = selectedIndex?.takeIf { it in 0 until state.layoutInfo.totalItemsCount }
                    ?: return@SmallFloatingActionButton
                scope.launch {
                    val nearbyItemCount = state.layoutInfo.visibleItemsInfo.size.coerceAtLeast(1) * 3
                    if (abs(index - state.firstVisibleItemIndex) <= nearbyItemCount) {
                        state.animateScrollToItem(index)
                    } else {
                        state.scrollToItem(index)
                    }
                }
            },
            shape = CircleShape
        ) {
            Icon(MaterialSymbols.Outlined.My_location, contentDescription = "定位到当前播放")
        }
    }
}
