package com.dhs0319.bills.feature.video.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.PlaybackSidePanel
import com.dhs0319.bills.core.playback.subtitle.VideoSubtitleState

@Composable
internal fun VideoSubtitlePanel(
    state: VideoSubtitleState,
    onDismiss: () -> Unit,
    onSelectTrack: (String?) -> Unit,
    onSelectSecondaryTrack: (String) -> Unit,
    onBilingualChange: (Boolean) -> Unit,
    onLargeTextChange: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    PlaybackSidePanel("字幕", onDismiss, modifier, embedded) { close ->
        val secondaryTracks = state.secondaryTracks
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item("primary_tracks") {
                Column(Modifier.fillMaxWidth().subtitleCard().selectableGroup().padding(vertical = 8.dp)) {
                    SubtitleTrackRow("关闭", state.selectedTrackId == null) { close { onSelectTrack(null) } }
                    if (state.loadingTracks) {
                        SubtitleLoadingRow("正在加载字幕列表")
                    }
                    state.tracks.forEach { track ->
                        SubtitleTrackRow(track.displayName, track.id == state.selectedTrackId) {
                            close { onSelectTrack(track.id) }
                        }
                    }
                    if (state.tracksLoaded && state.tracks.isEmpty()) {
                        Text("此视频暂无可用字幕", modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (state.loadingContent) {
                item("loading_content") { SubtitleLoadingRow("正在加载字幕") }
            }
            state.error?.let { error ->
                item("error") {
                    Column(Modifier.fillMaxWidth().subtitleCard().padding(16.dp)) {
                        Text(error, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRetry) { Text("重试") }
                    }
                }
            }
            item("bilingual") {
                SubtitleSwitchRow(
                    title = "双语字幕",
                    checked = state.bilingual,
                    enabled = state.bilingual || secondaryTracks.isNotEmpty(),
                    supportingText = when {
                        state.selectedTrackId == null -> "先选择主字幕"
                        secondaryTracks.isEmpty() -> "当前视频没有其他语言字幕"
                        else -> null
                    },
                    onCheckedChange = onBilingualChange
                )
            }
            if (state.bilingual && secondaryTracks.isNotEmpty()) {
                item("secondary_title") {
                    Text("副字幕", style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }
                item("secondary_tracks") {
                    Column(Modifier.fillMaxWidth().subtitleCard().selectableGroup().padding(vertical = 8.dp)) {
                        secondaryTracks.forEach { track ->
                            SubtitleTrackRow(track.displayName, track.id == state.secondaryTrackId) {
                                onSelectSecondaryTrack(track.id)
                            }
                        }
                    }
                }
            }
            item("large_text") {
                SubtitleSwitchRow("字幕大字号", state.largeText, onCheckedChange = onLargeTextChange)
            }
        }
    }
}

@Composable
private fun Modifier.subtitleCard(): Modifier =
    clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)

@Composable
private fun SubtitleTrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SubtitleSwitchRow(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    supportingText: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().subtitleCard()
            .heightIn(min = 64.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SubtitleLoadingRow(label: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
