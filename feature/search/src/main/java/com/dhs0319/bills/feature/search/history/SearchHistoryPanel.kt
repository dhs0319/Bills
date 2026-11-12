package com.dhs0319.bills.feature.search.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Close
import com.composables.icons.materialsymbols.outlined.Delete
import com.composables.icons.materialsymbols.outlined.Expand_less
import com.composables.icons.materialsymbols.outlined.Expand_more
import com.dhs0319.bills.feature.search.landing.SearchSectionHeader

@Stable
internal class SearchHistoryPanelState(
    expanded: Boolean = false,
    editing: Boolean = false,
    confirmClear: Boolean = false
) {
    var expanded by mutableStateOf(expanded)
    var editing by mutableStateOf(editing)
    var confirmClear by mutableStateOf(confirmClear)
}

@Composable
internal fun rememberSearchHistoryPanelState(): SearchHistoryPanelState {
    return rememberSaveable(
        saver = listSaver(
            save = { listOf(it.expanded, it.editing, it.confirmClear) },
            restore = { SearchHistoryPanelState(it[0], it[1], it[2]) }
        )
    ) { SearchHistoryPanelState() }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun SearchHistoryPanel(
    histories: List<String>,
    state: SearchHistoryPanelState,
    onSearch: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val showAll = state.expanded || state.editing
    // Each chip has at least 24.dp of horizontal padding, so this safely covers two rows.
    val collapsedLimit = (LocalConfiguration.current.screenWidthDp / 24 + 1) * 2
    val visibleHistories = remember(histories, showAll, collapsedLimit) {
        if (showAll) histories else histories.take(collapsedLimit)
    }

    LaunchedEffect(histories.isEmpty()) {
        if (histories.isEmpty()) {
            state.editing = false
            state.confirmClear = false
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SearchSectionHeader(title = "搜索历史") {
            if (histories.isNotEmpty()) {
                if (state.editing) {
                    TextButton(onClick = { state.confirmClear = true }) {
                        Text("全部删除")
                    }
                    TextButton(onClick = { state.editing = false }) {
                        Text("完成")
                    }
                } else {
                    IconButton(onClick = { state.editing = true }) {
                        Icon(
                            imageVector = MaterialSymbols.Outlined.Delete,
                            contentDescription = "管理搜索历史",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { state.expanded = !state.expanded }) {
                        Icon(
                            imageVector = if (state.expanded) {
                                MaterialSymbols.Outlined.Expand_less
                            } else {
                                MaterialSymbols.Outlined.Expand_more
                            },
                            contentDescription = if (state.expanded) "收起搜索历史" else "展开搜索历史",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (histories.isEmpty()) {
            Text(
                text = "暂无搜索历史",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxLines = if (showAll) Int.MAX_VALUE else 2
            ) {
                visibleHistories.forEach { keyword ->
                    Surface(
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .combinedClickable(
                                role = Role.Button,
                                onClickLabel = if (state.editing) "删除历史搜索：$keyword" else "搜索：$keyword",
                                onClick = {
                                    if (state.editing) onDelete(keyword) else onSearch(keyword)
                                },
                                onLongClickLabel = "管理搜索历史",
                                onLongClick = { state.editing = true }
                            ),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = keyword,
                                modifier = Modifier.weight(1f, fill = false),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (state.editing) {
                                Icon(
                                    imageVector = MaterialSymbols.Outlined.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SearchHistoryActions(
    state: SearchHistoryPanelState,
    onClear: () -> Unit
) {
    BackHandler(enabled = state.editing && !state.confirmClear) {
        state.editing = false
    }

    if (state.confirmClear) {
        AlertDialog(
            onDismissRequest = { state.confirmClear = false },
            title = { Text("清空搜索历史？") },
            text = { Text("清空后无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    state.confirmClear = false
                    state.editing = false
                    state.expanded = false
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { state.confirmClear = false }) { Text("取消") }
            }
        )
    }
}
