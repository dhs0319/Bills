package com.dhs0319.bills.infra.player.danmaku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanmakuPlayerTimeProviderTest {
    @Test
    fun playingClockUsesSpeedAndPausedClockStaysAtItsAnchor() {
        var now = 100L
        val clock = DanmakuPlayerTimeProvider(1_000L, true, 2f, elapsedRealtime = { now })
        now = 400L
        assertEquals(1_600L, clock.getCurrentTimeMs())
        clock.overrideState(1_600L, false, 2f)
        now = 1_000L
        assertEquals(1_600L, clock.getCurrentTimeMs())
        assertFalse(clock.isPlaying())
    }

    @Test
    fun speedChangeCanKeepCurrentPositionContinuous() {
        var now = 0L
        val clock = DanmakuPlayerTimeProvider(1_000L, true, 1f, elapsedRealtime = { now })
        now = 500L
        clock.overrideState(clock.getCurrentTimeMs(), true, 3f)
        assertEquals(1_500L, clock.getCurrentTimeMs())
        now = 700L
        assertEquals(2_100L, clock.getCurrentTimeMs())
        assertTrue(clock.isPlaying())
    }

    @Test
    fun readUsesOneSnapshotEvenIfAnchorChangesDuringElapsedTimeRead() {
        var now = 0L
        var duringRead: (() -> Unit)? = null
        val clock = DanmakuPlayerTimeProvider(1_000L, true, 1f, elapsedRealtime = {
            duringRead?.let { callback ->
                duringRead = null
                callback()
            }
            now
        })
        now = 500L
        duringRead = { clock.overrideState(10_000L, true, 3f) }
        assertEquals(1_500L, clock.getCurrentTimeMs())
        assertEquals(10_000L, clock.getCurrentTimeMs())
    }

    @Test
    fun negativePositionSpeedAndElapsedDeltaAreClamped() {
        var now = 100L
        val clock = DanmakuPlayerTimeProvider(-1L, true, -2f, elapsedRealtime = { now })
        now = 50L
        assertEquals(0L, clock.getCurrentTimeMs())
        now = 200L
        assertEquals(0L, clock.getCurrentTimeMs())
        clock.overrideState(-20L, false, 1f)
        assertEquals(0L, clock.getCurrentTimeMs())
    }
}
