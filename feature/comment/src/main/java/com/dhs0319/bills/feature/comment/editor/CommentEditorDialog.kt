package com.dhs0319.bills.feature.comment.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CommentEditorDialog(
    panel: CommentEditorPanel,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAction: (CommentEditorAction) -> Unit,
    emotes: @Composable (Modifier) -> Unit,
    content: @Composable ((CommentEditorAction) -> Unit) -> Unit
) {
    // Set this before dispatching the action, so the very first keyboard frame
    // still has the same reserved space as the emote panel.
    var handingOffToKeyboard by remember { mutableStateOf(false) }
    val dispatchAction: (CommentEditorAction) -> Unit = remember(panel, onAction) {
        { action ->
            if (panel == CommentEditorPanel.Emotes && (
                    action == CommentEditorAction.ShowKeyboard ||
                        action == CommentEditorAction.ToggleEmotes ||
                        action == CommentEditorAction.OpenMentions
                    )
            ) {
                handingOffToKeyboard = true
            } else if (action == CommentEditorAction.ToggleEmotes) {
                handingOffToKeyboard = false
            }
            onAction(action)
        }
    }
    val dismiss = remember(busy, onDismiss) { { if (!busy) onDismiss() } }

    Dialog(
        onDismissRequest = dismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        BackHandler {
            if (!busy) {
                if (panel != CommentEditorPanel.Keyboard) {
                    dispatchAction(CommentEditorAction.ShowKeyboard)
                } else {
                    onDismiss()
                }
            }
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
        ) {
            val density = LocalDensity.current
            val ime = WindowInsets.ime
            val imeSource = WindowInsets.imeAnimationSource
            val imeTarget = WindowInsets.imeAnimationTarget
            val navigationBarHeightPx = WindowInsets.navigationBars.getBottom(density)
            val imeTargetHeightPx = imeTarget.getBottom(density)
            var lastKeyboardHeightPx by rememberSaveable(maxWidth, maxHeight, density.density) {
                mutableIntStateOf(0)
            }
            // The animation target is the full keyboard height, unlike the
            // intermediate IME insets reported while it slides in or out.
            SideEffect {
                if (panel != CommentEditorPanel.Emotes && imeTargetHeightPx > navigationBarHeightPx) {
                    lastKeyboardHeightPx = imeTargetHeightPx
                }
            }
            LaunchedEffect(panel, handingOffToKeyboard, density) {
                if (panel != CommentEditorPanel.Emotes && handingOffToKeyboard) {
                    // A hardware/floating keyboard may never produce a bottom
                    // inset. Do not leave an inactive emote panel visible then.
                    withTimeoutOrNull(1_500L) {
                        snapshotFlow {
                            val target = imeTarget.getBottom(density)
                            target > navigationBarHeightPx &&
                                ime.getBottom(density) == target &&
                                imeSource.getBottom(density) == target
                        }.first { it }
                    }
                    handingOffToKeyboard = false
                }
            }

            val fallbackHeightPx = with(density) {
                (maxHeight * 0.4f).coerceIn(200.dp, 360.dp).roundToPx() + navigationBarHeightPx
            }
            val keyboardHeightPx = when {
                imeTargetHeightPx > navigationBarHeightPx -> imeTargetHeightPx
                lastKeyboardHeightPx > 0 -> lastKeyboardHeightPx
                else -> fallbackHeightPx
            }
            val showEmotes = panel == CommentEditorPanel.Emotes || handingOffToKeyboard
            // Keyboard insets already include the navigation bar. Reserve this
            // area once, rather than adding imePadding to the whole dialog.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "关闭评论框" }
                    .clickable(
                        enabled = !busy,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = dismiss
                    )
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .semantics { paneTitle = "评论编辑" }
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Box(Modifier.verticalScroll(rememberScrollState())) {
                        content(dispatchAction)
                    }
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .layout { measurable, constraints ->
                            // Read animated insets during measurement, so each
                            // keyboard frame does not recompose the editor/grid.
                            val imeHeightPx = ime.getBottom(this)
                            val height = maxOf(
                                imeHeightPx,
                                navigationBarHeightPx,
                                if (showEmotes) keyboardHeightPx else 0
                            ).coerceIn(constraints.minHeight, constraints.maxHeight)
                            val placeable = measurable.measure(
                                constraints.copy(minHeight = height, maxHeight = height)
                            )
                            layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }
                        }
                        .clipToBounds(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    if (showEmotes) {
                        emotes(
                            Modifier.fillMaxSize().padding(
                                bottom = with(density) { navigationBarHeightPx.toDp() }
                            )
                        )
                    }
                }
            }
        }
    }
}
