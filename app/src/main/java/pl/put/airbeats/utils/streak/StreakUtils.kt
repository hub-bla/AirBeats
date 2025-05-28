package pl.put.airbeats.utils.streak

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

const val STREAK_FILE = "streak.json"

data class StreakData(
    var streak: Int,
    var lastUpdated: String // format: "YYYY-MM-DD"
)

fun writeStreakToFile(context: Context, data: String) {
    try {
        // MODE_PRIVATE means the file is only accessible by your app
        context.openFileOutput(STREAK_FILE, Context.MODE_PRIVATE).use { outputStream ->
            outputStream.write(data.toByteArray())
        }
        println("File saved successfully in internal storage.")
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun readStreak(context: Context): StreakData? {
    return try {
        val json = context.openFileInput(STREAK_FILE).bufferedReader().use { it.readText() }
        Gson().fromJson(json, StreakData::class.java)
    } catch (_: Exception) {
        null
    }
}

fun isTodayUpdated(streakData: StreakData): Boolean {
    val today = SimpleDateFormat("yyyy-MM-dd").format(Date())

    return today == streakData.lastUpdated
}

fun isYesterdayUpdated(streakData: StreakData): Boolean {
    val today = Calendar.getInstance()
    today.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = SimpleDateFormat("yyyy-MM-dd").format(today.time)

    return yesterday == streakData.lastUpdated
}

fun getStreak(context: Context): StreakData? {
    val streak: StreakData = readStreak(context) ?: return null
    if (!isYesterdayUpdated(streak) && !isTodayUpdated(streak)) {
        streak.streak = 0
        streak.lastUpdated = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val json = Gson().toJson(streak)

        writeStreakToFile(context, json)
    }

    return streak
}

fun updateStreak(context: Context) {
    var streak = readStreak(context)

    if (streak == null) {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
        streak = StreakData(1, today)
    }

    if (!isTodayUpdated(streak) && isYesterdayUpdated(streak)) {
        streak.streak += 1
        streak.lastUpdated = SimpleDateFormat("yyyy-MM-dd").format(Date())
    }

    if (isTodayUpdated(streak) && streak.streak == 0) {
        streak.streak = 1
    }
    if (!isTodayUpdated(streak) && !isYesterdayUpdated(streak)) {
        streak.streak = 0
        streak.lastUpdated = SimpleDateFormat("yyyy-MM-dd").format(Date())
    }

    val json = Gson().toJson(streak)

    writeStreakToFile(context, json)
}

