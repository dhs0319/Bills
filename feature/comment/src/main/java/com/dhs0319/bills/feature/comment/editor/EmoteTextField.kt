package com.dhs0319.bills.feature.comment.editor

import android.view.inputmethod.EditorInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dhs0319.bills.core.model.CommentEmote

/** Uses the comment editor's ImageSpan input, including atomic emote deletion and IME handling. */
@Composable
fun EmoteTextField(
    value: TextFieldValue,
    emotes: List<CommentEmote>,
    enabled: Boolean,
    showKeyboard: Boolean?,
    onValueChange: (TextFieldValue) -> Unit,
    onShowKeyboard: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String = "发消息"
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    val appearance = remember(colors, style, density) {
        CommentEditorAppearance(
            textColor = colors.onSurface.toArgb(),
            hintColor = colors.onSurfaceVariant.toArgb(),
            highlightColor = colors.primary.copy(alpha = 0.3f).toArgb(),
            cursorColor = colors.primary.toArgb(),
            textSize = with(density) { style.fontSize.toPx() },
            lineHeight = with(density) { style.lineHeight.toPx() },
            padding = with(density) { 12.dp.roundToPx() }
        )
    }
    AndroidView(
        factory = { context ->
            CommentEditorEditText(context).apply {
                minLines = 1
                maxLines = 4
                imeOptions = EditorInfo.IME_ACTION_SEND or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            }
        },
        modifier = modifier,
        onRelease = { it.releaseImages() },
        update = { view ->
            view.applyAppearance(appearance)
            if (view.hint != hint) view.hint = hint
            view.isEnabled = enabled
            view.onShowKeyboard = onShowKeyboard
            view.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (enabled) onSend()
                    true
                } else {
                    false
                }
            }
            view.bind(
                value = value,
                catalog = emotes,
                mentions = emptyList(),
                emoteSize = appearance.lineHeight.toInt(),
                mentionColor = colors.primary.toArgb(),
                onChange = onValueChange
            )
            view.updateKeyboard(showKeyboard)
        }
    )
}
