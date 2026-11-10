package com.dhs0319.bills.core.designsystem.icon

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Backspace
import com.composables.icons.materialsymbols.outlined.Open_in_new
import com.composables.icons.materialsymbols.outlinedfilled.Arrow_back
import com.composables.icons.materialsymbols.outlinedfilled.Chat
import com.composables.icons.materialsymbols.outlinedfilled.Exit_to_app
import com.composables.icons.materialsymbols.outlinedfilled.Keyboard_arrow_right
import com.composables.icons.materialsymbols.outlinedfilled.Send
import com.composables.icons.materialsymbols.outlinedfilled.Volume_up
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MirroredSymbolsTest {
    @Test
    fun directionalSymbolsRetainTheirGeometryAndEnableMirroring() {
        val symbols: List<Pair<ImageVector, ImageVector>> = listOf(
            MaterialSymbols.OutlinedFilled.Arrow_back to MirroredSymbols.ArrowBack,
            MaterialSymbols.Outlined.Backspace to MirroredSymbols.Backspace,
            MaterialSymbols.OutlinedFilled.Chat to MirroredSymbols.Chat,
            MaterialSymbols.OutlinedFilled.Exit_to_app to MirroredSymbols.ExitToApp,
            MaterialSymbols.OutlinedFilled.Keyboard_arrow_right to MirroredSymbols.KeyboardArrowRight,
            MaterialSymbols.Outlined.Open_in_new to MirroredSymbols.OpenInNew,
            MaterialSymbols.OutlinedFilled.Send to MirroredSymbols.Send,
            MaterialSymbols.OutlinedFilled.Volume_up to MirroredSymbols.VolumeUp
        )

        symbols.forEach { (original, mirrored) ->
            assertTrue(mirrored.autoMirror)
            assertEquals(original.name, mirrored.name)
            assertEquals(original.defaultWidth, mirrored.defaultWidth)
            assertEquals(original.defaultHeight, mirrored.defaultHeight)
            assertEquals(original.viewportWidth, mirrored.viewportWidth)
            assertEquals(original.viewportHeight, mirrored.viewportHeight)
            assertEquals(original.tintColor, mirrored.tintColor)
            assertEquals(original.tintBlendMode, mirrored.tintBlendMode)
            assertEquals(original.root, mirrored.root[0])
        }
    }
}
