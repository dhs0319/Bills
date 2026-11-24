package com.dhs0319.bills.feature.im.component

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

internal fun formatImSessionTime(
    micros: Long,
    nowMillis: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String {
    if (micros <= 0L) return ""
    val millis = TimeUnit.MICROSECONDS.toMillis(micros)
    val time = Instant.ofEpochMilli(millis).atZone(zone)
    val currentYear = Instant.ofEpochMilli(nowMillis).atZone(zone).year
    if (time.year < currentYear) return time.format(SESSION_DATE_FORMAT)

    val minutes = TimeUnit.MILLISECONDS.toMinutes((nowMillis - millis).coerceAtLeast(0L))
    val hours = TimeUnit.MINUTES.toHours(minutes)
    val days = TimeUnit.HOURS.toDays(hours)
    return when {
        minutes == 0L -> "刚刚"
        hours == 0L -> "$minutes 分钟前"
        days == 0L -> "$hours 小时前"
        days < 7L -> "$days 天前"
        else -> time.format(SESSION_DATE_TIME_FORMAT)
    }
}

private val SESSION_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MM-dd HH:mm")
private val SESSION_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
