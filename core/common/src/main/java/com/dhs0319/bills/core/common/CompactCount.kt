package com.dhs0319.bills.core.common

fun compactCount(value: String): String {
    val number = value.toLongOrNull() ?: return value
    return if (number < 10_000L) value else compactCount(number)
}

fun compactCount(number: Long): String = when {
    number < 10_000L -> number.toString()
    number < 100_000_000L -> scaledCount(number, 10_000L, "万")
    else -> scaledCount(number, 100_000_000L, "亿")
}

private fun scaledCount(number: Long, unit: Long, suffix: String): String {
    val tenths = (number % unit * 10L + unit / 2L) / unit
    val whole = number / unit + tenths / 10L
    val decimal = tenths % 10L
    return if (decimal == 0L) "$whole$suffix" else "$whole.$decimal$suffix"
}
