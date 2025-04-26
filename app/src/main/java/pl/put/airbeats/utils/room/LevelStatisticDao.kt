package pl.put.airbeats.utils.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelStatisticDao {
    @Query("SELECT * FROM Statistics")
    fun getAll(): Flow<List<LevelStatisticEntity>>

    @Query("SELECT * FROM Statistics WHERE userID = :userID")
    fun getUser(userID: String): Flow<List<LevelStatisticEntity>>

    @Insert
    suspend fun insert(statistic: LevelStatisticEntity)

    @Delete
    suspend fun delete(statistic: LevelStatisticEntity)
}