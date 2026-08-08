package com.naaammme.bbspace.core.designsystem.component

import android.widget.Toast
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString

@Suppress("DEPRECATION")
@Composable
fun Modifier.copyTextOnLongPress(
    text: String,
    label: String
): Modifier = if (text.isBlank()) {
    this
} else {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    pointerInput(text) {
        detectTapGestures(
            onLongPress = {
                clipboard.setText(AnnotatedString(text))
                Toast.makeText(context, "已复制$label", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
