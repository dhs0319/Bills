package com.dhs0319.bills.core.playback.subtitle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubtitleTimelineTest {
    @Test
    fun exactBoundariesAndGapsDoNotLeavePreviousCaptionOnScreen() {
        val timeline = SubtitleTimeline.create(listOf(
            SubtitleCue(100, 200, "first"), SubtitleCue(300, 400, "second")
        ))
        assertNull(timeline.cueAt(99))
        assertEquals(100L, timeline.nextChangeAfter(99))
        assertEquals("first", timeline.cueAt(100)?.text)
        assertEquals(200L, timeline.nextChangeAfter(100))
        assertNull(timeline.cueAt(200))
        assertEquals(300L, timeline.nextChangeAfter(200))
        assertEquals("second", timeline.cueAt(300)?.text)
        assertNull(timeline.cueAt(400))
        assertNull(timeline.nextChangeAfter(400))
    }

    @Test
    fun unsortedOverlappingCaptionsPreserveAllTextAndEachChangeBoundary() {
        val timeline = SubtitleTimeline.create(listOf(
            SubtitleCue(200, 400, "second"), SubtitleCue(100, 300, "first")
        ))
        assertEquals("first", timeline.cueAt(100)?.text)
        assertEquals(200L, timeline.nextChangeAfter(100))
        assertEquals("second\nfirst", timeline.cueAt(200)?.text)
        assertEquals(300L, timeline.nextChangeAfter(200))
        assertEquals("second", timeline.cueAt(300)?.text)
        assertNull(timeline.cueAt(400))
    }

    @Test
    fun adjacentIdenticalCaptionsMergeWithoutRedundantWakeups() {
        val timeline = SubtitleTimeline.create(listOf(
            SubtitleCue(100, 200, "same"), SubtitleCue(200, 300, "same"),
            SubtitleCue(150, 250, "same")
        ))
        assertEquals("same", timeline.cueAt(150)?.text)
        assertEquals(300L, timeline.nextChangeAfter(100))
    }

    @Test
    fun invalidAndEmptyEntriesAreIgnored() {
        val timeline = SubtitleTimeline.create(listOf(
            SubtitleCue(-1, 200, "invalid"), SubtitleCue(300, 100, "invalid"),
            SubtitleCue(100, 100, "invalid"), SubtitleCue(100, 200, " ")
        ))
        assertTrue(timeline.isEmpty)
        assertNull(timeline.cueAt(100))
        assertNull(timeline.nextChangeAfter(100))
    }

    @Test
    fun longVideoSupportsRandomForwardAndBackwardSeeks() {
        val timeline = SubtitleTimeline.create(List(50_000) { index ->
            SubtitleCue(index * 1000L, index * 1000L + 600L, "cue $index")
        })
        listOf(49_999, 0, 25_000, 2, 40_000, 1).forEach { index ->
            assertEquals("cue $index", timeline.cueAt(index * 1000L + 100)?.text)
            assertNull(timeline.cueAt(index * 1000L + 600))
        }
    }
}
