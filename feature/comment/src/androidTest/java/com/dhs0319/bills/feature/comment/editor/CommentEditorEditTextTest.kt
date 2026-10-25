package com.dhs0319.bills.feature.comment.editor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.View
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dhs0319.bills.core.model.CommentEmote
import com.dhs0319.bills.core.model.CommentUser
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommentEditorEditTextTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    private fun editor(value: String, onChange: (TextFieldValue) -> Unit = {}): CommentEditorEditText {
        val context = ContextThemeWrapper(instrumentation.targetContext, android.R.style.Theme_Material_Light_NoActionBar)
        val imageFile = File(context.cacheDir, "inline-emote-test.png")
        val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return CommentEditorEditText(context).apply {
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, 20f)
            bind(
                TextFieldValue(value, TextRange(value.length)),
                listOf(CommentEmote("[doge]", Uri.fromFile(imageFile).toString())),
                emptyList(), 40, Color.BLUE, onChange
            )
        }
    }

    private fun mentionEditor(value: String, cursor: Int, onChange: (TextFieldValue) -> Unit = {}): CommentEditorEditText {
        val context = ContextThemeWrapper(instrumentation.targetContext, android.R.style.Theme_Material_Light_NoActionBar)
        val user = CommentUser(42L, "猫哥捏食")
        return CommentEditorEditText(context).apply {
            bind(
                TextFieldValue(value, TextRange(cursor)),
                emptyList(),
                listOf(CommentEditorMention(user, TextRange(0, 5))),
                40, Color.BLUE, onChange
            )
        }
    }

    @Test fun imageSpansPreserveTheMessageAndSnapSelectionToImageEdges() = onMain {
        val view = editor("前[doge][doge]后")
        try {
            assertEquals("前[doge][doge]后", view.text.toString())
            assertEquals(2, view.text!!.getSpans(0, view.length(), EditorImageSpan::class.java).size)
            view.setSelection(2)
            assertEquals(1, view.selectionStart)
            view.setSelection(2, 10)
            assertEquals(1, view.selectionStart)
            assertEquals(13, view.selectionEnd)
            assertEquals("[doge][doge]", view.text!!.subSequence(view.selectionStart, view.selectionEnd).toString())
        } finally { view.releaseImages() }
    }

    @Test fun softKeyboardBackspaceDeletesAnEntireImageAndReportsRawText() = onMain {
        var lastValue = TextFieldValue()
        val view = editor("前[doge]") { lastValue = it }
        try {
            val connection = requireNotNull(view.onCreateInputConnection(EditorInfo()))
            assertTrue(connection.deleteSurroundingText(1, 0))
            assertEquals("前", view.text.toString())
            assertEquals("前", lastValue.text)
            assertEquals(TextRange(1), lastValue.selection)
        } finally { view.releaseImages() }
    }

    @Test fun softKeyboardBackspaceDeletesMentionAndItsTrailingSpaceAsOneUnit() = onMain {
        var changes = 0
        val view = mentionEditor("@猫哥捏食 ", 6) { changes++ }
        try {
            val connection = requireNotNull(view.onCreateInputConnection(EditorInfo()))
            assertTrue(connection.deleteSurroundingText(1, 0))
            assertEquals("", view.text.toString())
            assertEquals(0, view.selectionStart)
            assertEquals(1, changes)
        } finally { view.releaseImages() }
    }

    @Test fun backspaceAtMentionEndAlsoDeletesTheWholeMention() = onMain {
        val view = mentionEditor("@猫哥捏食", 5)
        try {
            assertTrue(view.onKeyDown(KeyEvent.KEYCODE_DEL, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL)))
            assertEquals("", view.text.toString())
        } finally { view.releaseImages() }
    }

    @Test fun codePointBackspaceDeletesMentionWithoutRemovingItsNeighbors() = onMain {
        val view = mentionEditor("@猫哥捏食 后文", 6)
        try {
            val connection = requireNotNull(view.onCreateInputConnection(EditorInfo()))
            assertTrue(connection.deleteSurroundingTextInCodePoints(1, 0))
            assertEquals("后文", view.text.toString())
            assertEquals(0, view.selectionStart)
        } finally { view.releaseImages() }
    }

    @Test fun composingTextBecomesAnImageOnlyAfterTheImeCommitsIt() = onMain {
        val view = editor("")
        try {
            val connection = requireNotNull(view.onCreateInputConnection(EditorInfo()))
            assertTrue(connection.setComposingText("[doge]", 1))
            assertEquals(0, view.text!!.getSpans(0, view.length(), EditorImageSpan::class.java).size)
            assertTrue(connection.finishComposingText())
            assertEquals(1, view.text!!.getSpans(0, view.length(), EditorImageSpan::class.java).size)
            assertEquals("[doge]", view.text.toString())
        } finally { view.releaseImages() }
    }

    @Test fun imageSpanReservesAndDrawsItsSquareInsteadOfDrawingTheToken() = onMain {
        val image = ColorDrawable(Color.MAGENTA).apply { setBounds(0, 0, 40, 40) }
        val span = EditorImageSpan(image, 40)
        val paint = Paint().apply { textSize = 20f }
        val output = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertEquals(40, span.getSize(paint, "[doge]", 0, 6, Paint.FontMetricsInt()))
        span.draw(Canvas(output), "[doge]", 0, 6, 10f, 0, 50, 100, paint)
        assertEquals(Color.MAGENTA, output.getPixel(25, 40))
        output.recycle()
    }

    @Test fun loadedImageIsDrawnInsideTheEditableText() {
        val view = onMain { editor("[doge]") }
        try {
            val deadline = System.currentTimeMillis() + 5_000L
            var drawn = false
            while (!drawn && System.currentTimeMillis() < deadline) {
                drawn = onMain {
                    view.measure(View.MeasureSpec.makeMeasureSpec(300, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.AT_MOST))
                    view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                    val output = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                    view.draw(Canvas(output))
                    val pixels = IntArray(output.width * output.height)
                    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
                    output.recycle()
                    pixels.any { it == Color.MAGENTA }
                }
                if (!drawn) Thread.sleep(25L)
            }
            assertTrue("The decoded bitmap should replace the loading placeholder inside EditText", drawn)
        } finally { onMain { view.releaseImages() } }
    }
}
