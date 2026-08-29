package com.dhs0319.bills.feature.search.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.SearchHistoryOrder

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHistoryPanel(
    histories: List<String>,
    order: SearchHistoryOrder,
    onSelectOrder: (SearchHistoryOrder) -> Unit,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var displayCap by remember { mutableStateOf(DISPLAY_STEP) }
    val visible = remember(histories, displayCap) {
        histories.take(displayCap)
    }
    val hasMore = histories.size > displayCap

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(
            key = "header",
            contentType = "header"
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "搜索历史",
                    style = MaterialTheme.typography.titleMedium
                )
                SearchHistoryOrderSelector(order = order, onSelect = onSelectOrder)
            }
        }

        item(
            key = "chips",
            contentType = "chips"
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                visible.forEachIndexed { index, item ->
                    SearchHistoryChip(
                        text = item,
                        featured = index == 0,
                        onClick = { onSearch(item) },
                        onLongClick = { onDelete(item) }
                    )
                }

                if (hasMore) {
                    val remaining = histories.size - displayCap
                    Surface(
                        onClick = { displayCap += DISPLAY_STEP },
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Text(
                            text = "展开更多 ($remaining)",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryOrderSelector(
    order: SearchHistoryOrder,
    onSelect: (SearchHistoryOrder) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf(
            SearchHistoryOrder.TIME to "按时间",
            SearchHistoryOrder.HOT to "按热度"
        ).forEachIndexed { index, (option, label) ->
            if (index > 0) {
                VerticalDivider(
                    modifier = Modifier.height(16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
            val selected = option == order
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                },
                modifier = Modifier
                    .selectable(
                        selected = selected,
                        onClick = { onSelect(option) },
                        role = Role.RadioButton,
                        interactionSource = remember(option) { MutableInteractionSource() },
                        indication = null
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchHistoryChip(
    text: String,
    featured: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        color = if (featured) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = if (featured) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val DISPLAY_STEP = 100
