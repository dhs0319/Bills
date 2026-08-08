package com.naaammme.bbspace.infra.player

import com.naaammme.bbspace.core.common.UserAgentBuilder

internal data class PlaybackRequestSpec(
    val userAgent: String,
    val headers: Map<String, String>
)

internal fun EngineSource.toPlaybackRequestSpec(): PlaybackRequestSpec {
    val useWebHeaders = when (this) {
        is EngineSource.LiveFlv -> false
        is EngineSource.LocalMerged -> false
        is EngineSource.SingleFileDash -> videoUrl.isWebPlaybackUrl() || audioUrl?.isWebPlaybackUrl() == true
        is EngineSource.Progressive -> segments.any { it.url.isWebPlaybackUrl() }
    }
    return if (useWebHeaders) {
        PlaybackRequestSpec(
            userAgent = UserAgentBuilder.buildWebUserAgent(),
            headers = WEB_HEADERS
        )
    } else {
        PlaybackRequestSpec(
            userAgent = UserAgentBuilder.buildPlayerUserAgent(),
            headers = emptyMap()
        )
    }
}

private fun String.isWebPlaybackUrl(): Boolean {
    return contains("platform=pc", ignoreCase = true)
}

private val WEB_HEADERS = mapOf("Referer" to "https://www.bilibili.com")
