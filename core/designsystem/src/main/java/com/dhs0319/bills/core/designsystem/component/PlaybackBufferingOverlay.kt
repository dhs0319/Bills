package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun PlaybackBufferingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    var delayedVisible by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            delay(BUFFERING_INDICATOR_DELAY_MS)
            delayedVisible = true
        } else {
            delayedVisible = false
        }
    }

    if (!delayedVisible) return

    CircularProgressIndicator(
        modifier = modifier
            .padding(16.dp)
            .size(36.dp),
        color = Color.White,
        strokeWidth = 3.dp
    )
}

fun formatPlaybackDownloadSpeed(bytesPerSecond: Long): String {
    val kilobytesPerSecond = bytesPerSecond.coerceAtLeast(0L) / 1024.0
    return if (kilobytesPerSecond >= 1024.0) {
        String.format(Locale.US, "%.1f MB/s", kilobytesPerSecond / 1024.0)
    } else {
        "${kilobytesPerSecond.toInt()} KB/s"
    }
}

private const val BUFFERING_INDICATOR_DELAY_MS = 300L
