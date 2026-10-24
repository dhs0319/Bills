package com.dhs0319.bills.feature.comment.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.BiliImageVariant

@Composable
internal fun CommentEmotePanel(
    state: CommentEmotePanelState,
    enabled: Boolean,
    onAction: (CommentEditorAction) -> Unit
) {
    val selected = state.packages.firstOrNull { it.id == state.selectedPackageId }
    val isRecent = state.selectedPackageId == -1L
    val emotes = if (isRecent) state.recent else selected?.emotes.orEmpty()
    val gridState = rememberLazyGridState()
    LaunchedEffect(state.selectedPackageId) { gridState.scrollToItem(0) }
    val panelHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.4f).coerceIn(200.dp, 360.dp)

    Column(modifier = Modifier.fillMaxWidth().height(panelHeight)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRecent) "最近使用" else selected?.name ?: "表情",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onAction(CommentEditorAction.ReloadEmotes) }, enabled = enabled && !state.loading) {
                Icon(Icons.Outlined.Refresh, "刷新表情", modifier = Modifier.size(18.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.loading && emotes.isEmpty() -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                state.error != null && emotes.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    TextButton(onClick = { onAction(CommentEditorAction.ReloadEmotes) }) { Text("重试") }
                }
                emotes.isEmpty() -> Text(
                    if (isRecent) "使用过的表情会出现在这里" else "暂无表情",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(if (selected?.isText == true) 96.dp else 48.dp),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(emotes, key = { it.text }) { emote ->
                        Box(
                            modifier = Modifier.height(48.dp)
                                .alpha(if (emote.available) 1f else 0.4f)
                                .clickable(enabled = enabled && emote.available, role = Role.Button) {
                                    onAction(CommentEditorAction.InsertEmote(emote))
                                }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (emote.url.isBlank()) {
                                Text(emote.text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            } else {
                                BiliAsyncImage(
                                    url = emote.url,
                                    contentDescription = emote.text,
                                    modifier = Modifier.size(if (emote.size > 1L) 36.dp else 30.dp),
                                    variant = BiliImageVariant.Original,
                                    contentScale = ContentScale.Fit
                                )
                            }
                            if (!emote.available) {
                                Icon(Icons.Outlined.Lock, "未解锁", modifier = Modifier.align(Alignment.BottomEnd).size(12.dp))
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                item(key = "recent") {
                    EmotePackageTab(selected = isRecent, enabled = enabled, onClick = { onAction(CommentEditorAction.SelectPackage(-1L)) }) {
                        Icon(Icons.Outlined.History, "最近使用", modifier = Modifier.size(24.dp))
                    }
                }
                items(state.packages, key = { it.id }) { pack ->
                    EmotePackageTab(selected = pack.id == state.selectedPackageId, enabled = enabled, onClick = { onAction(CommentEditorAction.SelectPackage(pack.id)) }) {
                        if (pack.iconUrl.isBlank()) {
                            Text(pack.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        } else {
                            BiliAsyncImage(pack.iconUrl, pack.name, Modifier.size(28.dp), BiliImageVariant.Original, ContentScale.Fit)
                        }
                    }
                }
            }
            IconButton(onClick = { onAction(CommentEditorAction.DeleteText) }, enabled = enabled) {
                Icon(Icons.AutoMirrored.Outlined.Backspace, "删除")
            }
        }
    }
}

@Composable
private fun EmotePackageTab(selected: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        modifier = Modifier.size(44.dp).selectable(
            selected = selected,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            enabled = enabled,
            role = Role.Tab,
            onClick = onClick
        )
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
