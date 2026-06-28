package com.na982.opichelper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QaItemEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun qaItemDao(): QaItemDao
}
