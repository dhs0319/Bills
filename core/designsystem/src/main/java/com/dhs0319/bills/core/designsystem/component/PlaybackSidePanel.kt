package com.dhs0319.bills.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import android.graphics.drawable.ColorDrawable
import kotlinx.coroutines.launch

/** Settings share their content between a fullscreen overlay and an ordinary bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSettingsPanel(
    title: String,
    onDismiss: () -> Unit,
    embedded: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (embedded) {
        PlaybackSidePanel(title = title, onDismiss = onDismiss, modifier = modifier, embedded = true) {
            content()
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val maxHeight = with(LocalDensity.current) {
            LocalWindowInfo.current.containerSize.height.toDp() * 0.7f
        }
        ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Box(Modifier.fillMaxWidth().weight(1f, fill = false)) { content() }
            }
        }
    }
}

/** A transparent player overlay with translucent content and the app's accent colors. */
@Composable
fun PlaybackSidePanel(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    content: @Composable (close: (() -> Unit) -> Unit) -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val close: (() -> Unit) -> Unit = { afterClose ->
        if (!closing) {
            closing = true
            scope.launch {
                if (!embedded) progress.animateTo(0f, tween(180))
                afterClose()
            }
        }
    }
    val dismiss = { close { currentOnDismiss() } }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(240)) }

    val panelColors = MaterialTheme.colorScheme.copy(
        surface = Color.Transparent,
        onSurface = Color(0xFFF5E9E5),
        onSurfaceVariant = Color(0xFFD7C9C5),
        surfaceVariant = Color(0xFF191719).copy(alpha = 0.68f),
        surfaceContainerHighest = Color(0xFF191719).copy(alpha = 0.68f)
    )
    val panelContent: @Composable () -> Unit = {
        MaterialTheme(colorScheme = panelColors) {
            CompositionLocalProvider(LocalContentColor provides panelColors.onSurface) {
                BoxWithConstraints(modifier = modifier.fillMaxSize()) {
                    val isCompactDevice = maxWidth < 600.dp
                    val panelWidth = if (maxWidth > maxHeight) {
                        // Phones need a wider panel for readable labels; tablets stay compact.
                        if (isCompactDevice) {
                            (maxWidth * 0.62f).coerceIn(280.dp, 420.dp)
                        } else {
                            (maxWidth * 0.36f).coerceIn(260.dp, 330.dp)
                        }
                    } else {
                        if (isCompactDevice) maxWidth * 0.82f else maxWidth * 0.58f
                    }.coerceAtMost(maxWidth)
                    val availableHeight = maxHeight
                    Box(
                        Modifier.fillMaxSize()
                            .padding(
                                top = if (embedded) 82.dp else 0.dp,
                                bottom = if (embedded) 80.dp else 0.dp
                            )
                    ) {
                        Box(
                            Modifier.fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = dismiss
                                )
                        )
                        Column(
                            Modifier.align(Alignment.CenterEnd)
                                .zIndex(10f)
                                .width(panelWidth)
                                .wrapContentHeight()
                                .heightIn(max = availableHeight * 0.88f)
                                .background(
                                    Color.Black.copy(alpha = 0.48f),
                                    RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                                )
                                .graphicsLayer { translationX = size.width * (1f - progress.value) }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {}
                                )
                                .windowInsetsPadding(
                                    WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
                                IconButton(onClick = dismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "关闭面板")
                                }
                            }
                            Box(
                                Modifier.fillMaxWidth()
                                    .heightIn(max = availableHeight * 0.76f)
                            ) { content(close) }
                        }
                    }
                }
            }
        }
    }
    if (embedded) {
        BackHandler(onBack = dismiss)
        panelContent()
    } else {
        Dialog(
            onDismissRequest = dismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            SideEffect {
                dialogWindow?.setDimAmount(0f)
                dialogWindow?.setBackgroundDrawable(ColorDrawable(Color.Transparent.toArgb()))
            }
            panelContent()
        }
    }
}
