package com.dhs0319.bills.feature.home.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UploaderBadge(modifier: Modifier = Modifier) {
    val authorStyle = MaterialTheme.typography.bodySmall
    val authorColor = MaterialTheme.colorScheme.onSurfaceVariant
    val badgeHeight = with(LocalDensity.current) { authorStyle.fontSize.toDp() }
    val badgeTextSize = authorStyle.fontSize * 0.6f

    Box(
        modifier = modifier
            .width(badgeHeight + 4.dp)
            .height(badgeHeight)
            .border(1.dp, lerp(authorColor, Color.White, 0.12f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "UP",
            color = authorColor,
            style = authorStyle.copy(
                fontSize = badgeTextSize,
                lineHeight = badgeTextSize,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1
        )
    }
}
