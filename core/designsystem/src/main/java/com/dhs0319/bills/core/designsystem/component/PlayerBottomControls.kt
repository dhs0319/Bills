package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PlayerBottomControls(
    isPlaying: Boolean,
    playContentDescription: String,
    timeText: String,
    progressValue: Float,
    bufferedProgressValue: Float = 0f,
    progressEnabled: Boolean,
    showProgress: Boolean = true,
    isFullscreen: Boolean,
    fullscreenContentDescription: String,
    onTogglePlay: () -> Unit,
    playEnabled: Boolean = true,
    onToggleFullscreen: () -> Unit,
    onProgressChange: (Float) -> Unit,
    onProgressChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    progressRowEndPadding: Dp = 28.dp,
    controlRowEndPadding: Dp = 20.dp,
    optionActions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.54f)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            if (showProgress) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = progressRowEndPadding),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = timeText,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    PlaybackProgressBar(
                        value = progressValue,
                        bufferedValue = bufferedProgressValue,
                        onValueChange = onProgressChange,
                        onValueChangeFinished = onProgressChangeFinished,
                        enabled = progressEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = controlRowEndPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    enabled = playEnabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = playContentDescription,
                        tint = if (playEnabled) Color.White else Color.White.copy(alpha = 0.45f)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                if (isFullscreen) {
                    optionActions()
                }
                IconButton(
                    onClick = onToggleFullscreen,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                        contentDescription = fullscreenContentDescription,
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerControlTextButton(
    text: String,
    onClick: () -> Unit,
    label: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val foreground = if (enabled) Color.White else Color.White.copy(alpha = 0.45f)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            label?.let {
                Text(
                    text = it,
                    color = foreground.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
            Text(
                text = text,
                color = foreground,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
