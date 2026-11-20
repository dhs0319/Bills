package com.dhs0319.bills.feature.video.detail

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private const val PLAYER_ASPECT = 16f / 9f

private enum class DetailSlot { Summary, Tabs }

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
    playerOnly: Boolean,
    player: @Composable (Modifier) -> Unit,
    detail: @Composable (summaryModifier: Modifier, tabsModifier: Modifier) -> Unit
) {
    val widePortrait = isExpandedWidth && !isLandscape
    val phoneLandscape = !playerOnly && isLandscape && isPhoneLandscape()
    val windowPadding = when {
        playerOnly -> Modifier
        // Reserve the space for system bars while they slide back in after fullscreen playback.
        isLandscape -> Modifier.windowInsetsPadding(
            WindowInsets.systemBarsIgnoringVisibility.union(WindowInsets.displayCutout).union(WindowInsets.ime)
        )
        widePortrait -> Modifier.statusBarsPadding().padding(16.dp)
        else -> Modifier
    }
    val playerFrame = when {
        playerOnly -> Modifier
        isLandscape -> Modifier.background(Color.Black)
        widePortrait -> Modifier.clip(MaterialTheme.shapes.extraLarge)
        else -> Modifier.background(Color.Black).statusBarsPadding()
    }
    val playerSize = if (playerOnly || widePortrait) Modifier.fillMaxSize() else Modifier.aspectRatio(PLAYER_ASPECT)

    Layout(
        modifier = Modifier.fillMaxSize()
            .background(if (playerOnly) Color.Black else MaterialTheme.colorScheme.background)
            .then(windowPadding),
        content = {
            // Keep the AndroidView attached when only the player's bounds change.
            Box(playerFrame, contentAlignment = Alignment.Center) {
                player(playerSize)
            }
            if (!playerOnly) {
                detail(Modifier.layoutId(DetailSlot.Summary), Modifier.layoutId(DetailSlot.Tabs))
            }
        }
    ) { measurables, constraints ->
        val playerMeasurable = measurables.first()
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        if (playerOnly) {
            val frame = playerMeasurable.measure(constraints)
            return@Layout layout(width, height) { frame.placeRelative(0, 0) }
        }

        val tabsMeasurable = measurables.first { it.layoutId == DetailSlot.Tabs }
        when {
            isLandscape -> {
                val gap = 8.dp.roundToPx().coerceAtMost(width)
                val columnWidth = ((width - gap) * if (phoneLandscape) 0.60f else 0.68f).roundToInt()
                val summaryHeight = if (phoneLandscape) height * 0.26f
                    else maxOf(height * 0.34f, minOf(164.dp.toPx(), height * 0.5f))
                val frame = playerMeasurable.measure(
                    Constraints(
                        minWidth = if (phoneLandscape) 0 else columnWidth,
                        maxWidth = columnWidth,
                        maxHeight = (height - summaryHeight).roundToInt()
                    )
                )
                val summary = measurables.first { it.layoutId == DetailSlot.Summary }
                    .measure(Constraints.fixed(frame.width, height - frame.height))
                val tabs = tabsMeasurable.measure(Constraints.fixed(width - frame.width - gap, height))
                layout(width, height) {
                    frame.placeRelative(0, 0)
                    summary.placeRelative(0, frame.height)
                    tabs.placeRelative(frame.width + gap, 0)
                }
            }

            widePortrait -> {
                val gap = 16.dp.roundToPx().coerceAtMost(width)
                val playerWidth = ((width - gap) * 0.54f).roundToInt()
                val frame = playerMeasurable.measure(Constraints.fixed(playerWidth, height))
                val tabs = tabsMeasurable.measure(Constraints.fixed(width - playerWidth - gap, height))
                layout(width, height) {
                    frame.placeRelative(0, 0)
                    tabs.placeRelative(playerWidth + gap, 0)
                }
            }

            else -> {
                val frame = playerMeasurable.measure(constraints.copy(minHeight = 0))
                val tabs = tabsMeasurable.measure(Constraints.fixed(width, height - frame.height))
                layout(width, height) {
                    frame.placeRelative(0, 0)
                    tabs.placeRelative(0, frame.height)
                }
            }
        }
    }
}
