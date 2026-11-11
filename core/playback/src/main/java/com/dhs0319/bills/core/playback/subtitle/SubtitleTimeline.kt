package com.dhs0319.bills.core.playback.subtitle

import androidx.compose.runtime.Immutable

@Immutable
data class SubtitleCue(val startMs: Long, val endMs: Long, val text: String)

/** Immutable, non-overlapping intervals. Build once off the main thread; lookups are O(log n). */
@Immutable
class SubtitleTimeline private constructor(private val cues: List<SubtitleCue>) {
    val isEmpty: Boolean get() = cues.isEmpty()

    fun cueAt(positionMs: Long): SubtitleCue? {
        return cues.getOrNull(indexAtOrBefore(positionMs))?.takeIf { positionMs < it.endMs }
    }

    fun nextChangeAfter(positionMs: Long): Long? {
        val index = indexAtOrBefore(positionMs)
        val current = cues.getOrNull(index)
        return if (current != null && positionMs < current.endMs) current.endMs
        else cues.getOrNull(index + 1)?.startMs
    }

    private fun indexAtOrBefore(positionMs: Long): Int {
        var low = 0
        var high = cues.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (cues[mid].startMs <= positionMs) low = mid + 1 else high = mid
        }
        return low - 1
    }

    companion object {
        val Empty = SubtitleTimeline(emptyList())

        fun create(input: List<SubtitleCue>): SubtitleTimeline {
            val valid = input.filter { it.startMs >= 0L && it.endMs > it.startMs && it.text.isNotBlank() }
            if (valid.isEmpty()) return Empty
            val events = ArrayList<Event>(valid.size * 2)
            valid.forEachIndexed { index, cue ->
                events += Event(cue.startMs, index, true)
                events += Event(cue.endMs, index, false)
            }
            events.sortBy { it.timeMs }
            val active = sortedMapOf<Int, String>()
            val intervals = ArrayList<SubtitleCue>(valid.size)
            var previousTime = events.first().timeMs
            var index = 0
            while (index < events.size) {
                val time = events[index].timeMs
                if (time > previousTime && active.isNotEmpty()) {
                    val text = active.values.distinct().joinToString("\n")
                    val previous = intervals.lastOrNull()
                    if (previous?.endMs == previousTime && previous.text == text) {
                        intervals[intervals.lastIndex] = previous.copy(endMs = time)
                    } else {
                        intervals += SubtitleCue(previousTime, time, text)
                    }
                }
                // Group equal timestamps so adjacent and overlapping captions have exact boundaries.
                while (index < events.size && events[index].timeMs == time) {
                    val event = events[index++]
                    if (event.start) active[event.index] = valid[event.index].text
                    else active.remove(event.index)
                }
                previousTime = time
            }
            return SubtitleTimeline(intervals)
        }
    }

    private data class Event(val timeMs: Long, val index: Int, val start: Boolean)
}
