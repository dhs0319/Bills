package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Immutable
data class AppUpdateDialogState(
    val title: String,
    val desc: String,
    val confirmText: String? = null,
    val url: String? = null
)

@Composable
fun AppUpdateDialog(
    state: AppUpdateDialogState,
    onDismiss: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val paragraphs = remember(state.desc) { parseUpdateNoteParagraphs(state.desc) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            SelectableText(state.title)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(scrollState)
            ) {
                paragraphs.forEach { paragraph ->
                    if (paragraph.marker == null) {
                        SelectableText(
                            text = paragraph.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = paragraph.marker,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SelectableText(
                                text = paragraph.text,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val url = state.url
                    if (url == null) {
                        onDismiss()
                    } else {
                        onOpenUrl(url)
                    }
                }
            ) {
                Text(state.confirmText ?: "知道了")
            }
        },
        dismissButton = if (state.url != null) {
            {
                TextButton(onClick = onDismiss) {
                    Text("关闭")
                }
            }
        } else {
            null
        }
    )
}

private data class UpdateNoteParagraph(
    val text: String,
    val marker: String? = null
)

private val UpdateNoteListItem = Regex("^([-*+])[ \\t]+(.*)$")

private fun parseUpdateNoteParagraphs(text: String): List<UpdateNoteParagraph> = buildList {
    text.lineSequence().forEach { line ->
        val listItem = UpdateNoteListItem.matchEntire(line)
        val previous = lastOrNull()
        when {
            listItem != null -> add(
                UpdateNoteParagraph(
                    text = listItem.groupValues[2],
                    marker = listItem.groupValues[1]
                )
            )
            previous?.marker != null && line.isNotBlank() &&
                (line.startsWith("  ") || line.startsWith('\t')) -> {
                // Keep explicitly indented continuation lines inside the item's text column.
                set(lastIndex, previous.copy(text = previous.text + "\n" + line.trimStart()))
            }
            else -> add(UpdateNoteParagraph(text = line))
        }
    }
}
