package com.dhs0319.bills.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.theme.BiliTheme
import com.dhs0319.bills.core.designsystem.theme.CornerStyle
import com.dhs0319.bills.core.designsystem.theme.ThemeConfig
import com.dhs0319.bills.core.designsystem.theme.ThemeMode

@Preview(widthDp = 420, heightDp = 700)
@Composable
internal fun VideoGridCardSkeletonCornersPreview() {
    BiliTheme(config = ThemeConfig(themeMode = ThemeMode.DARK)) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CornerStyle.entries.chunked(2).forEach { corners ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    corners.forEach { corner ->
                        BiliTheme(config = ThemeConfig(themeMode = ThemeMode.DARK, cornerStyle = corner)) {
                            Column(Modifier.weight(1f)) {
                                Text(corner.name, color = MaterialTheme.colorScheme.onBackground)
                                VideoGridCardSkeleton(style = VideoGridCardSkeletonStyle.AUDIO)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(widthDp = 420, heightDp = 700)
@Composable
internal fun VideoGridCardSkeletonStylesPreview() {
    BiliTheme(config = ThemeConfig(themeMode = ThemeMode.DARK)) {
        Column(
            modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            VideoGridCardSkeletonStyle.entries.chunked(2).forEach { styles ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    styles.forEach { style ->
                        Column(Modifier.weight(1f)) {
                            Text(style.name, color = MaterialTheme.colorScheme.onBackground)
                            VideoGridCardSkeleton(style = style)
                        }
                    }
                }
            }
        }
    }
}
