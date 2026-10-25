package com.dhs0319.bills.feature.comment.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.AvatarImage

@Composable
internal fun CommentMentionPanel(state: CommentMentionPickerState, onAction: (CommentEditorAction) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("选择要 @ 的人", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { onAction(CommentEditorAction.ShowKeyboard) }) { Text("返回") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = if (state.query.isBlank()) {
                    "直接在评论输入框中输入昵称搜索"
                } else {
                    "搜索：${state.query}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            when {
                state.loading -> CircularProgressIndicator(modifier = Modifier.size(28.dp))
                state.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    TextButton(onClick = { onAction(CommentEditorAction.SearchMentions(state.query)) }) { Text("重试") }
                }
                state.groups.isEmpty() -> Text(
                    if (state.query.isBlank()) "输入昵称搜索用户" else "没有找到相关用户",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    state.groups.forEachIndexed { groupIndex, group ->
                        if (group.name.isNotBlank()) {
                            item(key = "group_$groupIndex") {
                                Text(group.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 10.dp))
                            }
                        }
                        items(group.users, key = { "${groupIndex}_${it.mid}" }) { user ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { onAction(CommentEditorAction.InsertMention(user)) }
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AvatarImage(user.face, user.name, Modifier.size(40.dp))
                                Text(user.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
    }
}
