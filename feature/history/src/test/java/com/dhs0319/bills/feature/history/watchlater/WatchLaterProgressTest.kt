package com.dhs0319.bills.feature.history.watchlater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WatchLaterProgressTest {
    @Test
    fun partiallyWatchedVideoShowsElapsedAndTotalTimeWithMatchingProgress() {
        val progress = watchLaterProgress(progressSec = 220L, durationSec = 984L)

        assertNotNull(progress)
        assertEquals("03:40/16:24", progress?.timeText)
        assertEquals(220f / 984f, progress!!.fraction!!, 0.0001f)
    }

    @Test
    fun finishedSentinelShowsFullProgress() {
        assertEquals(
            WatchLaterProgress(timeText = "已看完", fraction = 1f),
            watchLaterProgress(progressSec = -1L, durationSec = 995L)
        )
    }

    @Test
    fun finishedSentinelDoesNotRequireDuration() {
        for (duration in listOf(null, 0L, -1L)) {
            assertEquals(
                WatchLaterProgress(timeText = "已看完", fraction = 1f),
                watchLaterProgress(progressSec = -1L, durationSec = duration)
            )
        }
    }

    @Test
    fun elapsedTimeAtOrBeyondDurationShowsFullProgress() {
        for (elapsed in listOf(984L, 1000L)) {
            assertEquals(
                WatchLaterProgress(timeText = "已看完", fraction = 1f),
                watchLaterProgress(progressSec = elapsed, durationSec = 984L)
            )
        }
    }

    @Test
    fun unplayedOrMissingProgressDoesNotShowProgressEvenWithMissingDuration() {
        for (elapsed in listOf(null, 0L, -2L)) {
            for (duration in listOf(null, 0L, 984L)) {
                assertNull(watchLaterProgress(progressSec = elapsed, durationSec = duration))
            }
        }
    }

    @Test
    fun missingDurationStillShowsElapsedTimeWithoutInventingProgressRatio() {
        for (duration in listOf(null, 0L, -1L)) {
            assertEquals(
                WatchLaterProgress(timeText = "已观看 03:40", fraction = null),
                watchLaterProgress(progressSec = 220L, durationSec = duration)
            )
        }
    }

    @Test
    fun hourLongVideoShowsFullTime() {
        assertEquals(
            "1:01:01/1:30:00",
            watchLaterProgress(progressSec = 3661L, durationSec = 5400L)?.timeText
        )
    }
}
