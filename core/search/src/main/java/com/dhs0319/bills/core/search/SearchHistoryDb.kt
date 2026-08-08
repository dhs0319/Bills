package com.dhs0319.bills.core.search

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SearchHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SearchHistoryDb : RoomDatabase() {
    abstract fun dao(): SearchHistoryDao
}
