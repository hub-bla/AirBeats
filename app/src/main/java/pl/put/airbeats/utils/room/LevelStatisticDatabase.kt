package pl.put.airbeats.utils.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [LevelStatisticEntity::class], version = 1)
abstract class LevelStatisticDatabase : RoomDatabase() {
    abstract fun dao() : LevelStatisticDao
}