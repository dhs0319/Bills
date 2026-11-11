package com.dhs0319.bills.core.playback.subtitle

import androidx.compose.runtime.Immutable

@Immutable
data class VideoSubtitleTrack(
    val id: String,
    val language: String,
    val name: String,
    val url: String,
    val isAi: Boolean
) {
    val displayName: String get() = if (isAi && !name.contains("AI", ignoreCase = true)) "$name · AI" else name
    val baseLanguage: String get() = language.removePrefix("ai-").substringBefore('-').lowercase()
}

@Immutable
data class VideoSubtitleState(
    val aid: Long = 0L,
    val cid: Long = 0L,
    val tracks: List<VideoSubtitleTrack> = emptyList(),
    val tracksLoaded: Boolean = false,
    val loadingTracks: Boolean = false,
    val selectedTrackId: String? = null,
    val secondaryTrackId: String? = null,
    val bilingual: Boolean = false,
    val largeText: Boolean = false,
    val loadingContent: Boolean = false,
    val primary: SubtitleTimeline? = null,
    val secondary: SubtitleTimeline? = null,
    val error: String? = null
) {
    val selectedTrack: VideoSubtitleTrack? get() = tracks.firstOrNull { it.id == selectedTrackId }
    val secondaryTrack: VideoSubtitleTrack? get() = tracks.firstOrNull { it.id == secondaryTrackId }
    val secondaryTracks: List<VideoSubtitleTrack> get() {
        val primaryLanguage = selectedTrack?.baseLanguage ?: return emptyList()
        return tracks.filter { it.baseLanguage != primaryLanguage }
    }
}
