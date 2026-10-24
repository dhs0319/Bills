package com.dhs0319.bills.feature.comment.component

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import com.dhs0319.bills.core.designsystem.component.BiliAsyncImage
import com.dhs0319.bills.core.designsystem.component.BiliImageVariant
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.CommentUser

private val COMMENT_EMOTE_REGEX = Regex("(\\[[^]]+])")
private const val MENTION_TAG = "comment_mention"

internal data class ParsedCommentText(
    val text: AnnotatedString,
    val emotes: List<ParsedCommentEmote>
)

internal data class ParsedCommentEmote(
    val id: String,
    val emote: CommentEmote
)

@Composable
@Suppress("DEPRECATION")
fun CommentRichText(
    text: String,
    emotes: List<CommentEmote>,
    modifier: Modifier = Modifier,
    style: TextStyle,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    copyLabel: String = "评论",
    onTap: (() -> Unit)? = null,
    mentions: List<CommentUser> = emptyList(),
    onOpenUser: ((CommentUser) -> Unit)? = null
) {
    val mentionColor = MaterialTheme.colorScheme.primary
    val parsed = remember(text, emotes, mentions, mentionColor) {
        parseCommentText(text, emotes, mentions, mentionColor)
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnOpenUser by rememberUpdatedState(onOpenUser)
    val currentMentions by rememberUpdatedState(mentions)
    val textModifier = modifier.pointerInput(text, parsed.text, copyLabel) {
        detectTapGestures(
            onTap = { position ->
                val offset = layoutResult?.getOffsetForPosition(position)
                val mid = offset?.let { parsed.text.getStringAnnotations(MENTION_TAG, it, it).firstOrNull()?.item?.toLongOrNull() }
                val user = currentMentions.firstOrNull { it.mid == mid }
                if (user != null && currentOnOpenUser != null) currentOnOpenUser?.invoke(user)
                else currentOnTap?.invoke()
            },
            onLongPress = {
                clipboard.setText(AnnotatedString(text))
                Toast.makeText(context, "已复制$copyLabel", Toast.LENGTH_SHORT).show()
            }
        )
    }
    val baseSize = style.lineHeight.takeIf { it.isSpecified }
        ?: MaterialTheme.typography.bodyMedium.lineHeight
    val inlineContent = remember(parsed.emotes, baseSize) {
        parsed.emotes.associate { item ->
            item.id to InlineTextContent(
                placeholder = Placeholder(
                    width = emoteSize(item.emote, baseSize),
                    height = emoteSize(item.emote, baseSize),
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                )
            ) {
                BiliAsyncImage(
                    url = item.emote.url,
                    contentDescription = item.emote.text,
                    modifier = Modifier.fillMaxSize(),
                    variant = BiliImageVariant.Original,
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
    Text(
        text = parsed.text,
        inlineContent = inlineContent,
        style = style,
        maxLines = maxLines,
        overflow = overflow,
        modifier = textModifier,
        onTextLayout = { layoutResult = it }
    )
}

internal fun parseCommentText(
    text: String,
    emotes: List<CommentEmote>,
    mentions: List<CommentUser> = emptyList(),
    mentionColor: Color = Color.Unspecified
): ParsedCommentText {
    val emoteMap = emotes.filter { it.url.isNotBlank() }.associateBy(CommentEmote::text)
    val builder = AnnotatedString.Builder()
    val parsedEmotes = mutableListOf<ParsedCommentEmote>()
    val seenEmoteIds = mutableSetOf<String>()
    var lastIndex = 0
    COMMENT_EMOTE_REGEX.findAll(text).forEach { match ->
        val emote = emoteMap[match.value] ?: return@forEach
        if (match.range.first > lastIndex) {
            builder.append(text.substring(lastIndex, match.range.first))
        }
        val id = "comment_emote_${emote.text}"
        builder.appendInlineContent(id, emote.text)
        if (seenEmoteIds.add(id)) {
            parsedEmotes += ParsedCommentEmote(id = id, emote = emote)
        }
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        builder.append(text.substring(lastIndex))
    }
    val users = mentions.filter { it.mid > 0L && it.name.isNotBlank() }.sortedByDescending { it.name.length }
    var mentionIndex = text.indexOf('@')
    while (mentionIndex >= 0) {
        val start = mentionIndex
        val user = users.firstOrNull { user ->
            val token = "@${user.name}"
            val end = start + token.length
            text.startsWith(token, start) && (end == text.length ||
                (!text[end].isLetterOrDigit() && text[end] != '_' && text[end] != '-'))
        }
        val end = user?.let { start + it.name.length + 1 } ?: (start + 1)
        if (user != null) {
            builder.addStyle(SpanStyle(color = mentionColor), start, end)
            builder.addStringAnnotation(MENTION_TAG, user.mid.toString(), start, end)
        }
        mentionIndex = text.indexOf('@', end)
    }
    return ParsedCommentText(
        text = builder.toAnnotatedString(),
        emotes = parsedEmotes
    )
}

private fun emoteSize(
    emote: CommentEmote,
    baseSize: TextUnit
): TextUnit {
    return if (emote.size > 1L) {
        baseSize * 1.5f
    } else {
        baseSize
    }
}
