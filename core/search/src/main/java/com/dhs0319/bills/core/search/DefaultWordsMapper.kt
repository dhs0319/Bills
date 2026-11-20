package com.dhs0319.bills.core.search

import com.bapis.bilibili.app.interfaces.v1.DefaultWordsReply
import com.dhs0319.bills.core.model.SearchDefaultWord

internal fun DefaultWordsReply.toSearchDefaultWord(): SearchDefaultWord? {
    val defaultWord = SearchDefaultWord(
        show = show.trim(),
        word = word.trim(),
        refreshIntervalMillis = refreshIntervalMilli
    )
    return defaultWord.takeIf { it.display.isNotBlank() }
}
