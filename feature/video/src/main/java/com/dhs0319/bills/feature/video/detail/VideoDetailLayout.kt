package com.dhs0319.bills.feature.video.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

private const val PLAYER_ASPECT = 16f / 9f

@Composable
internal fun isPhoneLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE &&
        configuration.smallestScreenWidthDp < 600
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VideoDetailLayout(
    isLandscape: Boolean,
    isExpandedWidth: Boolean,
    player: @Composable (Modifier) -> Unit,
    summary: @Composable (Modifier) -> Unit,
    tabs: @Composable () -> Unit
) {
    val background = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    when {
        // Reserve the bars' final space while they slide back in after fullscreen playback.
        isLandscape -> BoxWithConstraints(
            background.windowInsetsPadding(
                WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout).union(WindowInsets.ime)
            )
        ) {
            val phone = isPhoneLandscape()
            val columnWidthLimit = (maxWidth - 8.dp).coerceAtLeast(0.dp) * (if (phone) 0.60f else 0.68f)
            val summaryHeight = if (phone) maxHeight * 0.26f
                else maxOf(maxHeight * 0.34f, minOf(164.dp, maxHeight * 0.5f))
            val playerWidth = minOf(columnWidthLimit, (maxHeight - summaryHeight) * PLAYER_ASPECT)
            val leftWidth = if (phone) playerWidth else columnWidthLimit
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.width(leftWidth).fillMaxHeight()) {
                    Box(Modifier.fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
                        player(Modifier.width(playerWidth).aspectRatio(PLAYER_ASPECT))
                    }
                    summary(
                        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                Box(Modifier.weight(1f)) { tabs() }
            }
        }

        isExpandedWidth -> Row(
            modifier = background.statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            player(Modifier.weight(0.54f).fillMaxHeight().clip(MaterialTheme.shapes.extraLarge))
            Box(Modifier.weight(0.46f)) { tabs() }
        }

        else -> Column(background) {
            Box(Modifier.fillMaxWidth().background(Color.Black).statusBarsPadding()) {
                player(Modifier.fillMaxWidth().aspectRatio(PLAYER_ASPECT))
            }
            Box(Modifier.weight(1f)) { tabs() }
        }
    }
}
