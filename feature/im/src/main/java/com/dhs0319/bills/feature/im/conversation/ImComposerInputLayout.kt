package com.dhs0319.bills.feature.im.conversation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationSource
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** Reserves the same area for emotes and the IME, including the navigation bar exactly once. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ImComposerInputLayout(
    emotes: @Composable (Modifier) -> Unit,
    content: @Composable (Boolean, Boolean?, () -> Unit, () -> Unit) -> Unit
) {
    var showEmotes by rememberSaveable { mutableStateOf(false) }
    var keyboardRequest by remember { mutableStateOf<Boolean?>(null) }
    var handingOff by remember { mutableStateOf(false) }
    val showKeyboard = {
        handingOff = showEmotes
        showEmotes = false
        keyboardRequest = true
    }
    val toggleEmotes = {
        if (showEmotes) {
            showKeyboard()
        } else {
            handingOff = false
            showEmotes = true
            keyboardRequest = false
        }
    }
    BackHandler(enabled = showEmotes || handingOff) {
        showEmotes = false
        handingOff = false
        keyboardRequest = null
    }
    val imeVisible = WindowInsets.isImeVisible
    var imeWasVisible by remember { mutableStateOf(false) }
    LaunchedEffect(imeVisible) {
        if (imeWasVisible && !imeVisible && !showEmotes && !handingOff) keyboardRequest = null
        imeWasVisible = imeVisible
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        val density = LocalDensity.current
        val ime = WindowInsets.ime
        val imeSource = WindowInsets.imeAnimationSource
        val imeTarget = WindowInsets.imeAnimationTarget
        val navHeightPx = WindowInsets.navigationBars.getBottom(density)
        val targetHeightPx = imeTarget.getBottom(density)
        var lastKeyboardHeightPx by rememberSaveable(maxWidth, maxHeight, density.density) { mutableIntStateOf(0) }
        SideEffect {
            if (!showEmotes && targetHeightPx > navHeightPx) lastKeyboardHeightPx = targetHeightPx
        }
        LaunchedEffect(showEmotes, handingOff, density) {
            if (!showEmotes && handingOff) {
                withTimeoutOrNull(1_500L) {
                    snapshotFlow {
                        val target = imeTarget.getBottom(density)
                        target > navHeightPx && ime.getBottom(density) == target && imeSource.getBottom(density) == target
                    }.first { it }
                }
                handingOff = false
            }
        }
        val fallbackHeightPx = with(density) { (maxHeight * 0.4f).coerceIn(200.dp, 320.dp).roundToPx() + navHeightPx }
        val panelHeightPx = if (lastKeyboardHeightPx > 0) lastKeyboardHeightPx else fallbackHeightPx
        val displayEmotes = showEmotes || handingOff
        Column {
            content(showEmotes, if (showEmotes) false else keyboardRequest, toggleEmotes, showKeyboard)
            Box(
                modifier = Modifier.fillMaxWidth().layout { measurable, constraints ->
                    val height = maxOf(ime.getBottom(this), navHeightPx, if (displayEmotes) panelHeightPx else 0)
                        .coerceIn(constraints.minHeight, constraints.maxHeight)
                    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
                    layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                }.clipToBounds()
            ) {
                if (displayEmotes) {
                    emotes(Modifier.fillMaxSize().padding(bottom = with(density) { navHeightPx.toDp() }))
                }
            }
        }
    }
}
