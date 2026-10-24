package com.dhs0319.bills.feature.comment.editor

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.asImage
import coil3.intercept.Interceptor
import coil3.request.SuccessResult
import com.dhs0319.bills.core.model.CommentEmote
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

class CommentEditorTestActivity : Activity()

@RunWith(AndroidJUnit4::class)
class CommentEditorImageRenderingTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun <T> onMain(block: () -> T): T {
        var result: Result<T>? = null
        instrumentation.runOnMainSync { result = runCatching(block) }
        return result!!.getOrThrow()
    }

    @Test fun delayedImageAndLaterFramesRefreshTheHardwareRenderedText() {
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, CommentEditorTestActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        val ready = CompletableDeferred<Unit>()
        val drawable = ColorDrawable(Color.MAGENTA)
        val loader = ImageLoader.Builder(activity).components {
            add(Interceptor { chain ->
                ready.await()
                SuccessResult(drawable.asImage(), chain.request)
            })
        }.build()
        var changes = 0
        val view = onMain {
            CommentEditorEditText(activity, loader).apply {
                showSoftInputOnFocus = false
                isCursorVisible = false
                setBackgroundColor(Color.BLACK)
                bind(
                    TextFieldValue("[doge]", TextRange(6)),
                    listOf(CommentEmote("[doge]", "test://delayed-emote")),
                    emptyList(), 80, Color.BLUE
                ) { changes++ }
                activity.setContentView(this)
            }
        }
        try {
            instrumentation.waitForIdleSync()
            assertTrue(onMain { view.isHardwareAccelerated })
            // Capture the placeholder before releasing the image request, so the
            // text's hardware display list already exists when onSuccess runs.
            assertFalse(editorContainsColor(view, Color.MAGENTA))
            ready.complete(Unit)
            assertTrue("Loaded images must replace the cached placeholder without typing", awaitEditorColor(view, Color.MAGENTA))
            onMain { drawable.color = Color.CYAN }
            assertTrue("Drawable frame updates must also invalidate the cached text", awaitEditorColor(view, Color.CYAN))
            onMain {
                assertEquals("[doge]", view.text.toString())
                assertEquals(6, view.selectionStart)
                assertEquals(0, changes)
            }
        } finally {
            onMain { view.releaseImages(); activity.finish() }
            loader.shutdown()
        }
    }

    private fun awaitEditorColor(view: CommentEditorEditText, color: Int): Boolean {
        val deadline = System.currentTimeMillis() + 3_000L
        while (System.currentTimeMillis() < deadline) {
            if (editorContainsColor(view, color)) return true
            Thread.sleep(25L)
        }
        return false
    }

    private fun editorContainsColor(view: CommentEditorEditText, color: Int): Boolean {
        val bounds = onMain {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
        }
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            val width = bounds.width().coerceAtMost(screenshot.width - bounds.left)
            val height = bounds.height().coerceAtMost(screenshot.height - bounds.top)
            val pixels = IntArray(width * height)
            screenshot.getPixels(pixels, 0, width, bounds.left, bounds.top, width, height)
            return pixels.any { it == color }
        } finally { screenshot.recycle() }
    }
}
