package com.dhs0319.bills.core.published

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PublishedRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PublishedRecordDb : RoomDatabase() {
    abstract fun dao(): PublishedRecordDao
}
