package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Immutable
data class PlaybackOption(
    val id: String,
    val label: String,
    val selected: Boolean,
    val needVip: Boolean = false,
    val supportingText: String? = null
)

fun shouldShowPlaybackSidebar(isFullscreen: Boolean): Boolean = isFullscreen

// Both entry points share the window-sized overlay, including embedded playback.
@Composable
fun PlaybackOptionBottomSheet(
    title: String,
    options: List<PlaybackOption>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    PlaybackOptionSidebar(title, options, onDismiss, onSelect)
}

@Composable
fun PlaybackOptionSidebar(
    title: String,
    options: List<PlaybackOption>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    key(title) {
        PlaybackSidePanel(title = title, onDismiss = onDismiss, modifier = modifier, embedded = embedded) { close ->
            val selectedIndex = options.indexOfFirst { it.selected }
            val selectedId = options.getOrNull(selectedIndex)?.id
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0)
            )
            LaunchedEffect(selectedId, selectedIndex) {
                if (selectedIndex >= 0) {
                    listState.scrollToItem(selectedIndex)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .selectableGroup(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(options, key = { it.id }) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option.selected,
                                role = Role.RadioButton,
                                onClick = { close { onSelect(option.id) } }
                            )
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option.selected, onClick = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (option.selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (option.needVip) {
                                    Text(
                                        text = "大会员",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = MaterialTheme.shapes.extraSmall
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            option.supportingText?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
