package com.dhs0319.bills.feature.video.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Chevron_right
import com.dhs0319.bills.core.designsystem.component.AnimatedLiveBadge
import com.dhs0319.bills.core.model.VideoPagePart

@Composable
internal fun VideoPageSelector(
    pages: List<VideoPagePart>,
    curCid: Long?,
    onSwitchPage: (Long) -> Unit,
    onShowAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = remember(pages, curCid) { pages.indexOfFirst { it.cid == curCid } }
    val currentPage = pages.getOrNull(selectedIndex)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val selectedCid = currentPage?.cid
    var previousSelectedCid by rememberSaveable { mutableStateOf(selectedCid) }
    LaunchedEffect(selectedCid) {
        if (selectedCid != null && selectedCid != previousSelectedCid) {
            val layout = state.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
            if (item == null || item.offset < layout.viewportStartOffset ||
                item.offset + item.size > layout.viewportEndOffset
            ) {
                state.scrollToItem(selectedIndex)
            }
        }
        previousSelectedCid = selectedCid
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "选集",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                currentPage?.let { page ->
                    Text(
                        text = "正在播放：${page.part}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TextButton(
                onClick = onShowAll,
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("共${pages.size}集", fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = MaterialSymbols.Outlined.Chevron_right,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 4.dp).size(18.dp)
                )
            }
        }
        LazyRow(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(pages, key = { _, page -> page.cid }, contentType = { _, _ -> "page_option" }) { index, page ->
                VideoPageOption(
                    page = page,
                    index = index,
                    selected = page.cid == curCid,
                    onClick = { if (page.cid != curCid) onSwitchPage(page.cid) }
                )
            }
        }
    }
}

@Composable
private fun VideoPageOption(
    page: VideoPagePart,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val titleColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Card(
        onClick = onClick,
        modifier = Modifier.width(144.dp).semantics { this.selected = selected },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Box(Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 22.dp)
                    .heightIn(min = 40.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected) {
                    AnimatedLiveBadge(
                        modifier = Modifier.size(16.dp),
                        tint = titleColor,
                        contentDescription = null
                    )
                }
                Text(
                    text = page.part,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                    color = titleColor,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = (index + 1).toString(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .clip(RoundedCornerShape(topStart = 6.dp))
                    .background(titleColor.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
                style = MaterialTheme.typography.labelSmall,
                color = titleColor
            )
        }
    }
}
