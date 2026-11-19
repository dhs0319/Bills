package com.dhs0319.bills.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil3.ComponentRegistry
import coil3.compose.AsyncImage
import coil3.map.Mapper
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.size.pxOrElse
import com.dhs0319.bills.core.common.media.BILI_IMAGE_DEFAULT_Q
import com.dhs0319.bills.core.common.media.avatarThumbnailUrl
import com.dhs0319.bills.core.common.media.coverThumbnailUrl
import com.dhs0319.bills.core.common.media.originImageUrl
import com.dhs0319.bills.core.common.media.playerCoverUrl
import com.dhs0319.bills.core.common.media.thumbnailUrl
import com.dhs0319.bills.core.designsystem.R

enum class BiliImageVariant(
    val q: Int = BILI_IMAGE_DEFAULT_Q,
    val original: Boolean = false
) {
    Avatar,
    CardCover,
    PlayerCover,
    PreviewThumb,
    Banner,
    Original(original = true)
}

private data class PlayerCoverImage(val url: String)

fun ComponentRegistry.Builder.addPlayerCoverMapper() {
    add(Mapper<PlayerCoverImage, String> { cover, options ->
        playerCoverUrl(cover.url, options.size.width.pxOrElse { Int.MAX_VALUE })
    })
}

@Composable
fun rememberBiliImageRequest(
    url: String?,
    variant: BiliImageVariant = BiliImageVariant.CardCover
): ImageRequest? {
    if (url.isNullOrBlank()) return null
    val context = LocalContext.current
    return remember(context, url, variant) {
        val data = when {
            variant.original -> originImageUrl(url)
            variant == BiliImageVariant.Avatar -> avatarThumbnailUrl(url)
            variant == BiliImageVariant.CardCover -> coverThumbnailUrl(url)
            variant == BiliImageVariant.PlayerCover -> PlayerCoverImage(url)
            else -> thumbnailUrl(url, variant.q)
        }
        ImageRequest.Builder(context)
            .data(data)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}

@Composable
fun BiliAsyncImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    variant: BiliImageVariant = BiliImageVariant.CardCover,
    contentScale: ContentScale = ContentScale.Crop
) {
    val request = rememberBiliImageRequest(url = url, variant = variant)
    val avatarPlaceholder = if (variant == BiliImageVariant.Avatar) {
        painterResource(R.drawable.noface)
    } else {
        null
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        placeholder = avatarPlaceholder,
        error = avatarPlaceholder,
        fallback = avatarPlaceholder
    )
}
