package com.dhs0319.bills.feature.video.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.outlined.Chevron_right
import com.dhs0319.bills.core.designsystem.component.AvatarImage
import com.dhs0319.bills.core.model.VideoStaff

@Composable
internal fun VideoTeamPreview(
    members: List<VideoStaff>,
    followingByMid: Map<Long, Boolean>,
    followBusyMids: Set<Long>,
    isSelf: (Long) -> Boolean,
    onOpenTeam: () -> Unit,
    onOpenMember: (VideoStaff) -> Unit,
    onFollowClick: (Long) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = "查看完整创作团队", role = Role.Button, onClick = onOpenTeam)
                .heightIn(min = 40.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "创作团队",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "共 ${members.size} 位",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = MaterialSymbols.Outlined.Chevron_right,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(members) { member ->
                Surface(
                    modifier = Modifier.width(178.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    TeamMemberRow(
                        member = member,
                        following = followingByMid[member.mid] ?: member.isFollowing,
                        followBusy = member.mid in followBusyMids,
                        isSelf = isSelf,
                        onOpenMember = onOpenMember,
                        onFollowClick = onFollowClick,
                        compact = true,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoTeamSheet(
    members: List<VideoStaff>,
    followingByMid: Map<Long, Boolean>,
    followBusyMids: Set<Long>,
    isSelf: (Long) -> Boolean,
    onOpenMember: (VideoStaff) -> Unit,
    onFollowClick: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "创作团队",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "共 ${members.size} 位",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(members) { member ->
                        TeamMemberRow(
                            member = member,
                            following = followingByMid[member.mid] ?: member.isFollowing,
                            followBusy = member.mid in followBusyMids,
                            isSelf = isSelf,
                            onOpenMember = onOpenMember,
                            onFollowClick = onFollowClick,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamMemberRow(
    member: VideoStaff,
    following: Boolean,
    followBusy: Boolean,
    isSelf: (Long) -> Boolean,
    onOpenMember: (VideoStaff) -> Unit,
    onFollowClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(MaterialTheme.shapes.medium)
                .clickable(onClickLabel = "查看个人空间") { onOpenMember(member) }
                .heightIn(min = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarImage(
                url = member.face,
                contentDescription = member.name,
                modifier = Modifier.size(if (compact) 34.dp else 42.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = member.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (member.mid > 0L && !isSelf(member.mid)) {
            FollowButton(
                following = following,
                followedBy = member.isFollowedBy,
                busy = followBusy,
                onClick = { onFollowClick(member.mid) },
                compactName = member.name.takeIf { compact }
            )
        }
    }
}
