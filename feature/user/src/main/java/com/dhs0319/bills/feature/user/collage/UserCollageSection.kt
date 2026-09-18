package com.dhs0319.bills.feature.user.collage

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.model.SpaceRoute
import com.dhs0319.bills.core.model.User
import com.dhs0319.bills.feature.user.UserDest

@Immutable
internal data class UserCollagePalette(
    val canvasTop: Color,
    val canvasMid: Color,
    val canvasBottom: Color,
    val tilePrimary: Color,
    val tileSecondary: Color,
    val tileHighlight: Color,
    val tileSurface: Color,
    val tileSurfaceStrong: Color
)

@Composable
internal fun rememberUserCollagePalette(): UserCollagePalette {
    val colors = MaterialTheme.colorScheme
    return remember(
        colors.primaryContainer,
        colors.surfaceContainer,
        colors.surface,
        colors.surfaceContainerLow,
        colors.surfaceContainerHigh
    ) {
        UserCollagePalette(
            // Keep the surface hierarchy, but retain a soft wash of the selected theme color.
            canvasTop = colors.primaryContainer.copy(alpha = 0.52f).compositeOver(colors.surface),
            canvasMid = colors.primary.copy(alpha = 0.05f).compositeOver(colors.surfaceContainerLow),
            canvasBottom = colors.primaryContainer.copy(alpha = 0.26f).compositeOver(colors.surfaceContainer),
            tilePrimary = colors.primaryContainer.copy(alpha = 0.98f),
            tileSecondary = colors.surfaceContainerHigh,
            tileHighlight = colors.primaryContainer.copy(alpha = 0.72f),
            tileSurface = colors.surfaceContainerLow,
            tileSurfaceStrong = colors.surfaceContainerHigh.copy(alpha = 0.98f)
        )
    }
}

@Composable
internal fun rememberUserCollageBackgroundBrush(palette: UserCollagePalette): Brush {
    return remember(palette.canvasTop, palette.canvasMid, palette.canvasBottom) {
        Brush.verticalGradient(listOf(palette.canvasTop, palette.canvasMid, palette.canvasBottom))
    }
}

/** Fixed responsive layout for the profile page. The previous collage was draggable and
 * persisted offsets, which made the page difficult to scan and inconsistent after rotation. */
@Composable
internal fun UserCollageSection(
    user: User?,
    onNavigateToAccount: () -> Unit,
    onOpenSpace: (SpaceRoute) -> Unit,
    onNavigate: (UserDest) -> Unit,
    onNavigateToDownload: () -> Unit,
    palette: UserCollagePalette,
    modifier: Modifier = Modifier
) {
    val spaceRoute = user
        ?.takeUnless { it.mid <= 0L && it.name.isBlank() }
        ?.let { SpaceRoute(mid = it.mid, name = it.name.takeIf(String::isNotBlank)) }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        val landscape = maxWidth > maxHeight
        if (landscape) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                AccountPanel(
                    user = user,
                    onNavigateToAccount = onNavigateToAccount,
                    palette = palette,
                    onOpenSpace = spaceRoute?.let { { onOpenSpace(it) } },
                    modifier = Modifier
                        .weight(0.43f)
                        .fillMaxHeight()
                )
                EntryPanel(
                    onNavigateToDownload = onNavigateToDownload,
                    onNavigate = onNavigate,
                    palette = palette,
                    modifier = Modifier
                        .weight(0.57f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AccountPanel(
                    user = user,
                    onNavigateToAccount = onNavigateToAccount,
                    palette = palette,
                    onOpenSpace = spaceRoute?.let { { onOpenSpace(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                EntryPanel(
                    onNavigateToDownload = onNavigateToDownload,
                    onNavigate = onNavigate,
                    palette = palette,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AccountPanel(
    user: User?,
    onNavigateToAccount: () -> Unit,
    palette: UserCollagePalette,
    onOpenSpace: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .then(onOpenSpace?.let { Modifier.clickable(onClick = it) } ?: Modifier),
                shape = CircleShape,
                color = palette.tileSurfaceStrong
            ) {
                AvatarImage(
                    url = user?.avatar?.takeIf(String::isNotBlank),
                    contentDescription = user?.name ?: "未登录",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                val loggedOut = user == null
                Text(
                    text = user?.name?.takeIf(String::isNotBlank) ?: "点击登录",
                    style = if (loggedOut) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = if (loggedOut) {
                        Modifier.clickable(onClick = onNavigateToAccount)
                    } else {
                        Modifier
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (user == null) "登录后同步你的观看记录" else user.sign.ifBlank { "这个人很神秘，什么都没有写" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "硬币 ${user?.coins?.takeUnless { it == 0.0 } ?: "--"}   Lv${user?.level ?: 0}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccountStat("动态", user?.dynamic?.toString() ?: "--", Modifier.weight(1f))
            AccountStat("关注", user?.following?.toString() ?: "--", Modifier.weight(1f))
            AccountStat("粉丝", user?.follower?.toString() ?: "--", Modifier.weight(1f))
        }
    }
}

@Composable
private fun AccountStat(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EntryPanel(
    onNavigateToDownload: () -> Unit,
    onNavigate: (UserDest) -> Unit,
    palette: UserCollagePalette,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntryTile(
                icon = Icons.Outlined.SaveAlt,
                title = "离线缓存",
                subtitle = "本地视频",
                color = palette.tilePrimary,
                onClick = onNavigateToDownload,
                modifier = Modifier.weight(1f)
            )
            EntryTile(
                icon = Icons.Outlined.History,
                title = "观看记录",
                subtitle = "继续观看",
                color = palette.tileSurface,
                onClick = { onNavigate(UserDest.History) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EntryTile(
                icon = Icons.Outlined.StarBorder,
                title = "我的收藏",
                subtitle = "收藏夹",
                color = palette.tileSecondary,
                onClick = { onNavigate(UserDest.Favorite) },
                modifier = Modifier.weight(1f)
            )
            EntryTile(
                icon = Icons.Outlined.WatchLater,
                title = "稍后再看",
                subtitle = "待看清单",
                color = palette.tileHighlight,
                onClick = { onNavigate(UserDest.WatchLater) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EntryTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 104.dp
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(minHeight),
        shape = MaterialTheme.shapes.large,
        color = color,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
