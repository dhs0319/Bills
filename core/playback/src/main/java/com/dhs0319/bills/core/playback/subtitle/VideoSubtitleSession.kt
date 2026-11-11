package com.dhs0319.bills.core.playback.subtitle

import com.dhs0319.bills.core.model.ResolvedVideoIds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Owned by the video ViewModel. Disabled subtitles do not collect progress or run a timer. */
class VideoSubtitleSession(
    private val scope: CoroutineScope,
    private val fetchTracks: suspend (ResolvedVideoIds) -> List<VideoSubtitleTrack>,
    private val fetchTimeline: suspend (VideoSubtitleTrack) -> SubtitleTimeline
) {
    constructor(scope: CoroutineScope, repository: VideoSubtitleRepository) :
        this(scope, repository::fetchTracks, repository::fetchTimeline)

    private val _state = MutableStateFlow(VideoSubtitleState())
    val state = _state.asStateFlow()
    private var source: ResolvedVideoIds? = null
    private var tracksJob: Job? = null
    private var contentJob: Job? = null
    private var generation = 0L
    // At most three parsed tracks, and only for the current video/part.
    private val cache = linkedMapOf<String, SubtitleTimeline>()

    fun setSource(ids: ResolvedVideoIds?) {
        val next = ids?.takeIf { it.aid > 0 && it.cid > 0 }?.let { ResolvedVideoIds(aid = it.aid, cid = it.cid) }
        if (source == next) return
        source = next
        generation++
        tracksJob?.cancel()
        tracksJob = null
        contentJob?.cancel()
        contentJob = null
        cache.clear()
        // Every video/part starts with subtitles off; metadata is loaded only on demand.
        _state.value = VideoSubtitleState(
            aid = next?.aid ?: 0,
            cid = next?.cid ?: 0,
            largeText = _state.value.largeText
        )
    }

    /** Reopening the same video also requires the user to enable subtitles again. */
    fun onVideoOpened() {
        selectTrack(null)
    }

    fun loadTracks() {
        val ids = source ?: return
        if (_state.value.tracksLoaded || tracksJob?.isActive == true) return
        val token = generation
        _state.value = _state.value.copy(loadingTracks = true, error = null)
        tracksJob = scope.launch {
            try {
                val tracks = fetchTracks(ids)
                currentCoroutineContext().ensureActive()
                if (token != generation) return@launch
                _state.value = _state.value.copy(tracks = tracks, tracksLoaded = true, loadingTracks = false)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (token == generation) {
                    _state.value = _state.value.copy(loadingTracks = false, error = "字幕列表加载失败，请重试")
                }
            }
        }
    }

    fun selectTrack(id: String?) {
        val track = _state.value.tracks.firstOrNull { it.id == id }
        if (id != null && track == null) return
        contentJob?.cancel()
        _state.value = _state.value.copy(
            selectedTrackId = track?.id, primary = null, secondary = null,
            secondaryTrackId = null, bilingual = track != null && _state.value.bilingual,
            loadingContent = false, error = null
        )
        if (track == null) return
        chooseSecondaryTrack()
        loadContent()
    }

    fun setBilingual(enabled: Boolean) {
        _state.value = _state.value.copy(bilingual = enabled, secondary = null, error = null)
        chooseSecondaryTrack()
        loadContent()
    }

    fun selectSecondaryTrack(id: String) {
        if (_state.value.secondaryTracks.none { it.id == id }) return
        _state.value = _state.value.copy(secondaryTrackId = id, secondary = null)
        loadContent()
    }

    fun setLargeText(enabled: Boolean) {
        _state.value = _state.value.copy(largeText = enabled)
    }

    fun retry() {
        if (!_state.value.tracksLoaded) loadTracks() else loadContent()
    }

    private fun chooseSecondaryTrack() {
        val state = _state.value
        val candidates = if (state.bilingual) state.secondaryTracks else emptyList()
        val secondary = candidates.firstOrNull { it.id == state.secondaryTrackId }
            ?: candidates.minByOrNull { it.isAi }
        _state.value = state.copy(secondaryTrackId = secondary?.id)
    }

    private fun loadContent() {
        contentJob?.cancel()
        val state = _state.value
        val primary = state.selectedTrack ?: return
        val secondary = state.secondaryTrack
        val token = generation
        _state.value = state.copy(loadingContent = true, error = null)
        contentJob = scope.launch {
            try {
                coroutineScope {
                    val secondaryLoad = secondary?.let {
                        async {
                            try {
                                Result.success(loadCached(it))
                            } catch (error: CancellationException) {
                                throw error
                            } catch (error: Exception) {
                                Result.failure(error)
                            }
                        }
                    }
                    val primaryTimeline = loadCached(primary)
                    currentCoroutineContext().ensureActive()
                    if (token != generation) return@coroutineScope
                    if (primaryTimeline.isEmpty) {
                        secondaryLoad?.cancel()
                        _state.value = _state.value.copy(
                            selectedTrackId = null, secondaryTrackId = null,
                            primary = null, secondary = null, loadingContent = false,
                            error = "这条字幕暂无内容"
                        )
                        return@coroutineScope
                    }
                    // Publish the primary without waiting for a slower secondary download.
                    _state.value = _state.value.copy(primary = primaryTimeline)
                    val secondaryResult = secondaryLoad?.await()
                    currentCoroutineContext().ensureActive()
                    if (token != generation) return@coroutineScope
                    _state.value = _state.value.copy(
                        secondary = secondaryResult?.getOrNull(), loadingContent = false,
                        error = if (secondaryResult?.isFailure == true) "副字幕加载失败，请重试" else null
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (token == generation) {
                    _state.value = _state.value.copy(loadingContent = false, error = "字幕加载失败，请重试")
                }
            }
        }
    }

    private suspend fun loadCached(track: VideoSubtitleTrack): SubtitleTimeline {
        cache[track.id]?.let { return it }
        val timeline = fetchTimeline(track)
        currentCoroutineContext().ensureActive()
        cache[track.id] = timeline
        while (cache.size > 3) cache.remove(cache.keys.first())
        return timeline
    }
}
