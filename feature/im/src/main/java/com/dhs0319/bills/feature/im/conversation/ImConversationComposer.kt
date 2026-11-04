package com.dhs0319.bills.feature.im.conversation

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.feature.comment.editor.EmotePicker
import com.dhs0319.bills.feature.comment.editor.EmoteTextField

@Composable
internal fun ImConversationComposer(
    state: ImConversationComposerState,
    onInputChange: (TextFieldValue) -> Unit,
    onSelectImage: (Uri?) -> Unit,
    onReloadEmotes: () -> Unit,
    onSelectEmotePackage: (Long) -> Unit,
    onInsertEmote: (CommentEmote) -> Unit,
    onDeleteText: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            // Keep the draft readable after process recreation or a long upload retry.
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            onSelectImage(uri)
        }
    }
    val canSend = state.input.text.isNotBlank() || state.selectedImageUri != null
    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface) {
        ImComposerInputLayout(
            emotes = { panelModifier ->
                EmotePicker(
                    state = state.emotePicker,
                    enabled = !state.isSending,
                    onReload = onReloadEmotes,
                    onSelectPackage = onSelectEmotePackage,
                    onInsert = onInsertEmote,
                    onDelete = onDeleteText,
                    modifier = panelModifier
                )
            }
        ) { showEmotes, keyboardRequest, onToggleEmotes, onShowKeyboard ->
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Column(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.selectedImageUri?.let { uri ->
                        Box(modifier = Modifier.padding(start = 52.dp, bottom = 4.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "待发送图片",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium)
                            )
                            IconButton(
                                onClick = { onSelectImage(null) },
                                enabled = !state.isSending,
                                modifier = Modifier.align(Alignment.TopEnd).size(32.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.Close, "移除图片", Modifier.size(18.dp))
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !state.isSending
                        ) {
                            Icon(Icons.Outlined.Image, if (state.selectedImageUri == null) "添加图片" else "更换图片")
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier.weight(1f)
                        ) {
                            EmoteTextField(
                                value = state.input,
                                emotes = state.emotes,
                                enabled = !state.isSending,
                                showKeyboard = keyboardRequest,
                                onValueChange = onInputChange,
                                onShowKeyboard = onShowKeyboard,
                                onSend = onSend,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp, max = 132.dp)
                            )
                        }
                        IconButton(
                            onClick = onToggleEmotes,
                            enabled = !state.isSending,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (showEmotes) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                            )
                        ) {
                            Icon(
                                if (showEmotes) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfied,
                                if (showEmotes) "切换键盘" else "选择表情"
                            )
                        }
                        if (canSend || state.isSending) {
                            Button(
                                onClick = onSend,
                                enabled = !state.isSending && canSend,
                                shape = MaterialTheme.shapes.extraLarge,
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                            ) {
                                if (state.isSending) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.padding(end = 6.dp).size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(if (state.isUploadingImage) "上传中" else if (state.isSending) "发送中" else "发送")
                            }
                        }
                    }
                    state.sendErrorMessage?.takeIf { it.isNotBlank() }?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 52.dp, end = 4.dp, bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
