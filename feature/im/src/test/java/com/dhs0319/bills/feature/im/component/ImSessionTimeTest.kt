package com.dhs0319.bills.feature.im.component

import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ImSessionTimeTest {
    private val now = Instant.parse("2026-11-24T12:00:00Z")
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun formatsRecentTimestampsByElapsedTime() {
        assertEquals("刚刚", format(now.minusSeconds(59)))
        assertEquals("1 分钟前", format(now.minusSeconds(60)))
        assertEquals("13 分钟前", format(now.minusSeconds(13 * 60)))
        assertEquals("1 小时前", format(now.minusSeconds(60 * 60)))
        assertEquals("1 天前", format(now.minusSeconds(24 * 60 * 60)))
        assertEquals("6 天前", format(now.minusSeconds(7 * 24 * 60 * 60 - 1)))
    }

    @Test
    fun keepsOlderTimestampsInTheLocalDateAndTimeFormat() {
        assertEquals("11-17 20:00", format(now.minusSeconds(7 * 24 * 60 * 60)))
    }

    @Test
    fun handlesMissingTimestampsAndClockSkew() {
        assertEquals("", formatImSessionTime(0, now.toEpochMilli(), zone))
        assertEquals("刚刚", format(now.plusSeconds(15)))
    }

    private fun format(time: Instant): String = formatImSessionTime(
        micros = TimeUnit.MILLISECONDS.toMicros(time.toEpochMilli()),
        nowMillis = now.toEpochMilli(),
        zone = zone
    )
}
