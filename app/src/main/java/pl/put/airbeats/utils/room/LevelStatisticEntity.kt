package pl.put.airbeats.utils.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Statistics")
data class LevelStatisticEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userID: String,
    val songName: String,
    val difficulty: String,
    val date: String,
    val points: Float,
    val perfect: Int,
    val great: Int,
    val good: Int,
    val missed: Int,
    val maxCombo: Int,
)