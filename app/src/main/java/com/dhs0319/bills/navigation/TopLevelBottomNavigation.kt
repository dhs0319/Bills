package com.dhs0319.bills.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.model.ImUnreadBadge

@Composable
internal fun TopLevelBottomNavigation(
    currentTab: TopLevelRoute,
    hidden: Boolean,
    onTabChange: (TopLevelRoute) -> Unit,
    messageUnreadBadge: ImUnreadBadge,
    modifier: Modifier = Modifier,
    onHeightChanged: (Dp) -> Unit = {}
) {
    val density = LocalDensity.current
    var heightPx by remember { mutableFloatStateOf(0f) }
    val offsetPx by animateFloatAsState(
        targetValue = if (hidden) heightPx else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "bottom menu offset"
    )

    Surface(
        modifier = modifier
            .onSizeChanged { size ->
                heightPx = size.height.toFloat()
                onHeightChanged(with(density) { size.height.toDp() })
            }
            .graphicsLayer { translationY = offsetPx },
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.navigationBars.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                    )
                )
                .selectableGroup()
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopLevelRoute.entries.forEach { tab ->
                TopLevelNavigationItem(
                    tab = tab,
                    selected = currentTab == tab,
                    messageUnreadBadge = if (tab == TopLevelRoute.MESSAGE) {
                        messageUnreadBadge
                    } else {
                        ImUnreadBadge.None
                    },
                    onClick = { onTabChange(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
