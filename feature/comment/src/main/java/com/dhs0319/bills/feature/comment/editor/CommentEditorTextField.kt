package com.dhs0319.bills.feature.comment.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.UpdateAppearance
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.dhs0319.bills.core.common.media.originImageUrl
import com.dhs0319.bills.core.model.CommentEmote

@Composable
internal fun CommentEditorTextField(
    state: CommentEditorState,
    onValueChange: (TextFieldValue) -> Unit,
    onShowKeyboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyLarge
    val density = LocalDensity.current
    val textSize = with(density) { style.fontSize.toPx() }
    val lineHeight = with(density) { style.lineHeight.toPx() }
    val padding = with(density) { 16.dp.roundToPx() }
    AndroidView(
        factory = { CommentEditorEditText(it) },
        modifier = modifier,
        onRelease = { it.releaseImages() },
        update = { view ->
            view.setTextColor(colors.onSurface.toArgb())
            view.setHintTextColor(colors.onSurfaceVariant.toArgb())
            view.highlightColor = colors.primary.copy(alpha = 0.3f).toArgb()
            if (view.textSize != textSize) view.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            view.setLineSpacing((lineHeight - view.paint.fontSpacing).coerceAtLeast(0f), 1f)
            view.setPadding(padding, padding, padding, padding)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.textCursorDrawable?.setTint(colors.primary.toArgb())
            }
            view.hint = state.target.parentName?.takeIf(String::isNotBlank)?.let { "回复 @$it" }
                ?: if (state.target.isReply) "输入回复内容" else "发一条友善的评论"
            view.isEnabled = !state.loading && !state.uploading
            view.onShowKeyboard = onShowKeyboard
            view.bind(
                value = state.input,
                catalog = state.emotes.packages.flatMap { it.emotes } + state.emotes.recent,
                mentions = state.mentions,
                emoteSize = lineHeight.toInt(),
                mentionColor = colors.primary.toArgb(),
                onChange = onValueChange
            )
            // The mention list shares this EditText. Keeping it focused avoids a
            // search-field -> editor focus hand-off (and the keyboard flicker it causes).
            view.updateKeyboard(state.panel != CommentEditorPanel.Emotes && view.isEnabled)
        }
    )
}

/** ImageSpan preserves the raw tokens for IME, clipboard and publication without an offset map. */
internal class CommentEditorEditText(
    context: Context,
    private val imageLoader: ImageLoader = SingletonImageLoader.get(context)
) : EditText(context) {
    var onShowKeyboard: (() -> Unit)? = null
    private var onChange: ((TextFieldValue) -> Unit)? = null
    private var updating = false
    private var changingText = false
    private var imeEditDepth = 0
    private var released = false
    private var keyboardRequested: Boolean? = null
    private var catalog: List<CommentEmote> = emptyList()
    private var mentions: List<CommentEditorMention> = emptyList()
    private var emotes: List<CommentEditorEmote> = emptyList()
    private var emoteSize = 1
    private var mentionColor = 0
    private var lastValue = TextFieldValue()
    private val images = mutableMapOf<String, EmoteImage>()
    private var imageRedrawPosted = false
    private val imageRedrawMarker = object : UpdateAppearance {}
    private val redrawImages = Runnable {
        imageRedrawPosted = false
        if (!released) {
            // EditText caches its text in separate hardware RenderNodes. A plain
            // invalidate() keeps those nodes (and their loading placeholders).
            // UpdateAppearance invalidates the text cache without replacing the
            // message, changing selection/composition, or reflowing its layout.
            text?.takeIf { it.isNotEmpty() }?.let { editable ->
                editable.setSpan(imageRedrawMarker, 0, editable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                editable.removeSpan(imageRedrawMarker)
            }
            invalidate()
        }
    }

    init {
        background = null
        typeface = Typeface.DEFAULT
        gravity = Gravity.TOP or Gravity.START
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        minLines = 4
        maxLines = 6
        includeFontPadding = false
        setHorizontallyScrolling(false)
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { changingText = true }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                changingText = false
                if (!updating && imeEditDepth == 0) refreshAndNotify()
            }
        })
    }

    fun bind(
        value: TextFieldValue,
        catalog: List<CommentEmote>,
        mentions: List<CommentEditorMention>,
        emoteSize: Int,
        mentionColor: Int,
        onChange: (TextFieldValue) -> Unit
    ) {
        this.onChange = onChange
        this.catalog = catalog
        this.mentions = mentions
        this.emoteSize = emoteSize.coerceAtLeast(1)
        this.mentionColor = mentionColor
        updating = true
        try {
            if (text.toString() != value.text) setText(value.text)
            renderSpans()
            val selection = snapEditorTokenSelection(value.selection, atomicRanges())
            if (selectionStart != selection.start || selectionEnd != selection.end) setSelection(selection.start, selection.end)
            lastValue = value
        } finally {
            updating = false
        }
        notifyValue()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (onChange == null || updating || changingText || imeEditDepth > 0 || selStart < 0 || selEnd < 0) return
        val snapped = snapEditorTokenSelection(TextRange(selStart, selEnd), atomicRanges())
        if (snapped.start != selStart || snapped.end != selEnd) {
            updating = true
            setSelection(snapped.start, snapped.end)
            updating = false
        }
        notifyValue()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN && isEnabled && !showSoftInputOnFocus) onShowKeyboard?.invoke()
        return super.onTouchEvent(event)
    }

    fun updateKeyboard(show: Boolean) {
        showSoftInputOnFocus = show
        if (keyboardRequested == show) return
        keyboardRequested = show
        val manager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (show) {
            post {
                if (!released && keyboardRequested == true) {
                    requestFocus()
                    manager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                }
            }
        } else {
            manager.hideSoftInputFromWindow(windowToken, 0)
        }
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val connection = super.onCreateInputConnection(outAttrs) ?: return null
        return object : InputConnectionWrapper(connection, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean = editFromIme {
                super.commitText(text, newCursorPosition)
            }
            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean = editFromIme {
                super.setComposingText(text, newCursorPosition)
            }
            override fun finishComposingText(): Boolean = editFromIme { super.finishComposingText() }
            override fun setComposingRegion(start: Int, end: Int): Boolean = editFromIme { super.setComposingRegion(start, end) }
            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean = editFromIme {
                deleteMention(beforeLength, afterLength) ||
                    deleteEmote(beforeLength, afterLength, codePoints = false) ||
                    super.deleteSurroundingText(beforeLength, afterLength)
            }
            override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int): Boolean = editFromIme {
                deleteMention(beforeLength, afterLength, codePoints = true) ||
                    deleteEmote(beforeLength, afterLength, codePoints = true) ||
                    super.deleteSurroundingTextInCodePoints(beforeLength, afterLength)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DEL && editFromIme { deleteMention(1, 0) }) return true
        if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL && editFromIme { deleteMention(0, 1) }) return true
        if (keyCode == KeyEvent.KEYCODE_DEL && deleteEmote(1, 0, codePoints = true)) return true
        if (keyCode == KeyEvent.KEYCODE_FORWARD_DEL && deleteEmote(0, 1, codePoints = true)) return true
        return super.onKeyDown(keyCode, event)
    }

    /**
     * A mention is one editor unit. Bilibili inserts a trailing space after it, so a
     * backspace immediately after that space must remove the mention and the space in
     * one operation. This runs before the normal one-character IME deletion.
     */
    private fun deleteMention(before: Int, after: Int, codePoints: Boolean = false): Boolean {
        val editable = text ?: return false
        if (selectionStart != selectionEnd || selectionStart < 0 || before < 0 || after < 0) return false
        val cursor = selectionStart
        val beforeChars = if (codePoints) cursor - Character.offsetByCodePoints(editable, cursor,
            -minOf(before, Character.codePointCount(editable, 0, cursor))) else before
        val afterChars = if (codePoints) Character.offsetByCodePoints(editable, cursor,
            minOf(after, Character.codePointCount(editable, cursor, editable.length))) - cursor else after
        val range = mentionDeletionRange(editable.toString(), cursor, mentions, beforeChars, afterChars) ?: return false
        editable.delete(range.start, range.end)
        setSelection(range.start)
        return true
    }

    private fun atomicRanges(): List<TextRange> = emotes.map { it.range } +
        mentions.filter { it.isValidIn(text.toString()) }.map { it.range }

    private fun deleteEmote(before: Int, after: Int, codePoints: Boolean): Boolean {
        val editable = text ?: return false
        if (selectionStart != selectionEnd || selectionStart < 0 || before < 0 || after < 0) return false
        val cursor = selectionStart
        val start = if (codePoints) Character.offsetByCodePoints(editable, cursor, -minOf(before, Character.codePointCount(editable, 0, cursor)))
            else (cursor - before).coerceAtLeast(0)
        val end = if (codePoints) Character.offsetByCodePoints(editable, cursor, minOf(after, Character.codePointCount(editable, cursor, editable.length)))
            else (cursor.toLong() + after).coerceAtMost(editable.length.toLong()).toInt()
        val range = TextRange(start, end)
        if (emotes.none { range.min < it.range.end && range.max > it.range.start }) return false
        val expanded = expandEditorEmoteRange(range, emotes)
        editable.delete(expanded.min, expanded.max)
        setSelection(expanded.min)
        return true
    }

    private inline fun editFromIme(block: () -> Boolean): Boolean {
        imeEditDepth++
        return try { block() } finally {
            imeEditDepth--
            if (imeEditDepth == 0) refreshAndNotify()
        }
    }

    private fun refreshAndNotify() {
        if (updating || released) return
        updating = true
        try {
            mentions = reconcileCommentMentions(lastValue.text, text.toString(), mentions)
            renderSpans()
        } finally { updating = false }
        notifyValue()
    }

    private fun notifyValue() {
        if (updating || released || selectionStart < 0 || selectionEnd < 0) return
        val editable = text ?: return
        val composingStart = BaseInputConnection.getComposingSpanStart(editable)
        val composingEnd = BaseInputConnection.getComposingSpanEnd(editable)
        val value = TextFieldValue(
            editable.toString(), TextRange(selectionStart, selectionEnd),
            if (composingStart >= 0 && composingEnd > composingStart) TextRange(composingStart, composingEnd) else null
        )
        if (value == lastValue) return
        lastValue = value
        onChange?.invoke(value)
    }

    private fun renderSpans() {
        val editable = text ?: return
        editable.getSpans(0, editable.length, EditorImageSpan::class.java).forEach(editable::removeSpan)
        editable.getSpans(0, editable.length, EditorMentionSpan::class.java).forEach(editable::removeSpan)
        val composingStart = BaseInputConnection.getComposingSpanStart(editable)
        val composingEnd = BaseInputConnection.getComposingSpanEnd(editable)
        emotes = findEditorEmotes(editable.toString(), catalog).filter {
            composingStart < 0 || it.range.end <= composingStart || it.range.start >= composingEnd
        }
        val activeImages = mutableSetOf<String>()
        emotes.forEach { item ->
            val size = if (item.emote.size > 1) (emoteSize * 1.5f).toInt() else emoteSize
            val key = "${item.emote.url}@$size"
            activeImages += key
            val image = images[key] ?: loadImage(key, item.emote, size)
            if (!image.failed) editable.setSpan(EditorImageSpan(image.drawable, size), item.range.start, item.range.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        images.keys.filter { it !in activeImages }.forEach { images.remove(it)?.release() }
        mentions.filter { it.isValidIn(editable.toString()) }.forEach {
            editable.setSpan(EditorMentionSpan(mentionColor), it.range.start, it.range.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun loadImage(key: String, emote: CommentEmote, size: Int): EmoteImage {
        val image = EmoteImage(EditorEmoteDrawable(size, currentHintTextColor).apply { callback = this@CommentEditorEditText })
        images[key] = image
        image.request = imageLoader.enqueue(
            ImageRequest.Builder(context).data(originImageUrl(emote.url)).size(size, size).allowHardware(false)
                .listener(
                    onSuccess = { _, result ->
                        if (!released && images[key] === image) {
                            image.drawable.setImage(result.image.asDrawable(resources))
                        }
                    },
                    onError = { _, _ ->
                        if (!released && images[key] === image) {
                            image.failed = true
                            refreshAndNotify()
                            requestLayout()
                        }
                    }
                ).build()
        )
        return image
    }

    override fun verifyDrawable(who: Drawable): Boolean =
        (onChange != null && images.values.any { it.drawable === who }) || super.verifyDrawable(who)

    override fun invalidateDrawable(drawable: Drawable) {
        if (onChange != null && images.values.any { it.drawable === drawable }) {
            // Coalesce loaded images and animation frames; never edit spans from
            // inside Drawable.draw(), which can also trigger this callback.
            if (!released && !imageRedrawPosted) {
                imageRedrawPosted = true
                postOnAnimation(redrawImages)
            }
        } else {
            super.invalidateDrawable(drawable)
        }
    }

    fun releaseImages() {
        released = true
        removeCallbacks(redrawImages)
        onChange = null
        onShowKeyboard = null
        images.values.forEach { it.release() }
        images.clear()
    }

    private class EmoteImage(val drawable: EditorEmoteDrawable, var request: Disposable? = null, var failed: Boolean = false) {
        fun release() {
            request?.dispose()
            drawable.setImage(null)
            drawable.callback = null
        }
    }
}

private class EditorMentionSpan(color: Int) : ForegroundColorSpan(color)

internal class EditorImageSpan(drawable: Drawable, private val size: Int) : ImageSpan(drawable) {
    override fun getSize(paint: Paint, text: CharSequence?, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        val metrics = paint.fontMetricsInt
        val center = (metrics.ascent + metrics.descent) / 2
        fm?.apply {
            ascent = minOf(metrics.ascent, center - size / 2)
            descent = maxOf(metrics.descent, ascent + size)
            top = ascent
            bottom = descent
        }
        return size
    }

    override fun draw(canvas: Canvas, text: CharSequence?, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint) {
        val metrics = paint.fontMetricsInt
        val imageTop = y + (metrics.ascent + metrics.descent - size) / 2f
        val save = canvas.save()
        canvas.translate(x, imageTop)
        drawable.draw(canvas)
        canvas.restoreToCount(save)
    }
}

private class EditorEmoteDrawable(private val size: Int, color: Int) : Drawable(), Drawable.Callback {
    private var image: Drawable? = null
    private val placeholderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; alpha = 48 }

    init { setBounds(0, 0, size, size) }

    fun setImage(value: Drawable?) {
        (image as? android.graphics.drawable.Animatable)?.stop()
        image?.callback = null
        image = value?.mutate()?.apply { callback = this@EditorEmoteDrawable }
        (image as? android.graphics.drawable.Animatable)?.start()
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val drawable = image
        if (drawable == null) {
            canvas.drawCircle(size / 2f, size / 2f, size * 0.3f, placeholderPaint)
            return
        }
        val width = drawable.intrinsicWidth.coerceAtLeast(1)
        val height = drawable.intrinsicHeight.coerceAtLeast(1)
        val scale = size.toFloat() / maxOf(width, height)
        val left = ((size - width * scale) / 2f).toInt()
        val top = ((size - height * scale) / 2f).toInt()
        drawable.setBounds(left, top, size - left, size - top)
        drawable.draw(canvas)
    }

    override fun invalidateDrawable(who: Drawable) = invalidateSelf()
    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) = scheduleSelf(what, `when`)
    override fun unscheduleDrawable(who: Drawable, what: Runnable) = unscheduleSelf(what)
    override fun setAlpha(alpha: Int) { image?.alpha = alpha }
    override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) { image?.colorFilter = colorFilter }
    @Suppress("DEPRECATION")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
}
