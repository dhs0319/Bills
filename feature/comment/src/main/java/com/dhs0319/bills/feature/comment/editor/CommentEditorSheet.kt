package com.dhs0319.bills.feature.comment.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun CommentEditorFab(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = contentDescription
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CommentEditorSheet(
    state: CommentEditorState,
    onDismiss: () -> Unit,
    onValueChange: (TextFieldValue) -> Unit,
    onSubmit: () -> Unit,
    onAddImages: (List<Uri>) -> Unit,
    onRemoveImage: (Int) -> Unit,
    onAction: (CommentEditorAction) -> Unit
) {
    if (!state.visible) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val busy = state.loading || state.uploading
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = COMMENT_EDITOR_MAX_IMAGE_COUNT)
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddImages(uris)
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        BackHandler(enabled = state.panel != CommentEditorPanel.Keyboard) {
            onAction(CommentEditorAction.ShowKeyboard)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CommentEditorTextField(
                state = state,
                onValueChange = onValueChange,
                onShowKeyboard = { onAction(CommentEditorAction.ShowKeyboard) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            if (state.selectedImageUris.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = state.selectedImageUris,
                        key = { _, uri -> uri }
                    ) { index, uri ->
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(72.dp)
                                .clip(MaterialTheme.shapes.small)
                        ) {
                            StaticPreviewImage(
                                uri = uri,
                                contentDescription = "图片${index + 1}",
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!busy) {
                                IconButton(
                                    onClick = { onRemoveImage(index) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "移除图片",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val showEmotes = state.panel == CommentEditorPanel.Emotes
                IconButton(
                    onClick = { onAction(CommentEditorAction.ToggleEmotes) },
                    enabled = !busy,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (showEmotes) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                ) {
                    Icon(
                        if (showEmotes) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfied,
                        contentDescription = if (showEmotes) "切换键盘" else "选择表情"
                    )
                }
                IconButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !busy && state.selectedImageUris.size < COMMENT_EDITOR_MAX_IMAGE_COUNT
                ) {
                    Icon(Icons.Outlined.Image, "添加图片")
                }
                IconButton(onClick = { onAction(CommentEditorAction.OpenMentions) }, enabled = !busy) {
                    Icon(Icons.Outlined.AlternateEmail, "提及用户")
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onSubmit,
                    enabled = !busy && state.input.text.isNotBlank(),
                    modifier = Modifier.padding(end = 8.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                ) {
                    Text(
                        when {
                            state.uploading -> "上传中"
                            state.loading -> "发送中"
                            else -> "发送"
                        }
                    )
                }
            }
            if (state.panel == CommentEditorPanel.Mentions) {
                CommentMentionPanel(state.mentionPicker, onAction)
            } else if (state.panel == CommentEditorPanel.Emotes) {
                CommentEmotePanel(state.emotes, enabled = !busy, onAction = onAction)
            }
        }
    }
}

@Composable
private fun StaticPreviewImage(
    uri: Uri,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(initialValue = null, context, uri) {
        value = withContext(Dispatchers.IO) {
            loadStaticPreview(context, uri)
        }
    }
    if (preview != null) {
        Image(
            bitmap = preview!!,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .height(72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "图片",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun loadStaticPreview(
    context: Context,
    uri: Uri
): ImageBitmap? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE)
            var sample = 1
            while (info.size.width / sample > PREVIEW_SIZE_PX || info.size.height / sample > PREVIEW_SIZE_PX) {
                sample = sample shl 1
            }
            decoder.setTargetSampleSize(sample)
        }.asImageBitmap()
    } else {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        } ?: return null
        var sample = 1
        while (boundsOpts.outWidth / sample > PREVIEW_SIZE_PX || boundsOpts.outHeight / sample > PREVIEW_SIZE_PX) {
            sample = sample shl 1
        }
        val bitmapOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bitmapOpts)?.asImageBitmap()
        }
    }
}

private const val PREVIEW_SIZE_PX = 144
