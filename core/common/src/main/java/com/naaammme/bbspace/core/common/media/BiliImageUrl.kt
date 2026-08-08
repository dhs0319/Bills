package com.naaammme.bbspace.core.common.media

private val thumbReg = Regex(
    pattern = """(@(\d+[a-z]_?)*)(\..*)?$""",
    options = setOf(RegexOption.IGNORE_CASE)
)

const val BILI_IMAGE_DEFAULT_Q = 15
private const val AVATAR_IMAGE_SUFFIX = "@120w_120h_85q_!widget-layer-avatar.webp"
private const val COVER_IMAGE_SUFFIX = "@575w_360h_1e_1c_85q.webp"

fun String.httpsImageUrl(): String {
    return when {
        startsWith("https://") -> this
        startsWith("http://") -> "https://${removePrefix("http://")}"
        else -> this
    }
}

fun String?.httpsImageUrlOrNull(): String? {
    return this?.httpsImageUrl()
}

fun coverThumbnailUrl(src: String?): String? {
    if (src.isNullOrBlank()) return src
    return src.httpsImageUrl().replaceImageBody { body ->
        body.substringBefore('@') + COVER_IMAGE_SUFFIX
    }
}

fun thumbnailUrl(src: String?, q: Int = BILI_IMAGE_DEFAULT_Q): String? {
    if (src.isNullOrBlank()) return src
    var matched = false
    val url = src.httpsImageUrl().replace(thumbReg) { match ->
        matched = true
        val suffix = match.groupValues[3].ifEmpty { ".webp" }
        "${match.groupValues[1]}_${q}q$suffix"
    }
    return if (matched) {
        url
    } else {
        "${url}@${q}q.webp"
    }
}

fun avatarThumbnailUrl(src: String?): String? {
    if (src.isNullOrBlank()) return src
    return src.httpsImageUrl().replaceImageBody { body ->
        body.substringBefore('@') + AVATAR_IMAGE_SUFFIX
    }
}

fun originImageUrl(src: String?): String? {
    if (src.isNullOrBlank()) return src
    return src.httpsImageUrl().replaceImageBody { body ->
        body.replace(thumbReg, "")
    }
}

private inline fun String.replaceImageBody(transform: (String) -> String): String {
    val queryIdx = indexOf('?')
    val body = if (queryIdx >= 0) substring(0, queryIdx) else this
    val query = if (queryIdx >= 0) substring(queryIdx) else ""
    return transform(body) + query
}
