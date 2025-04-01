package pl.put.airbeats.utils

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.*

data class Event(
    val eventId: Long,
    val eventTitle: String,
    val dayName: String,
    val startTime: String,
    val dayShortcut: String,
    val eventStartTime: Long,
    val eventEndTime: Long
)

fun getPrimaryCalendarId(contentResolver: ContentResolver): Long {
    val projection = arrayOf(
        CalendarContract.Calendars._ID,
        CalendarContract.Calendars.IS_PRIMARY
    )

    val cursor = contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        projection,
        null,
        null,
        null
    )

    cursor?.use {
        if (it.moveToFirst()) {
            do {
                val calendarIdIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
                val isPrimaryIndex = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                if (calendarIdIndex != -1 && isPrimaryIndex != -1) {
                    val calendarId = it.getLong(calendarIdIndex)
                    val isPrimary = it.getInt(isPrimaryIndex)

                    if (isPrimary == 1) {
                        return calendarId
                    }
                }
            } while (it.moveToNext())
        }
    }

    val fallbackCursor = contentResolver.query(
        CalendarContract.Calendars.CONTENT_URI,
        arrayOf(CalendarContract.Calendars._ID),
        null,
        null,
        null
    )

    fallbackCursor?.use {
        if (it.moveToFirst()) {
            val calendarIdIndex = it.getColumnIndex(CalendarContract.Calendars._ID)
            if (calendarIdIndex != -1) {
                return it.getLong(calendarIdIndex) // Return the first calendar ID as fallback
            }
        }
    }

    throw IllegalStateException("No calendars found on the device.")
}


fun createCalendarForRecurringEvent(
    context: Context,
    selectedDays: Set<Int>,
    selectedHour: Int,
    selectedMinute: Int
) {
    val calendarList = mutableListOf<Calendar>()
    val contentResolver = context.contentResolver
    val calendarId = getPrimaryCalendarId(contentResolver)
    val calendarUri = CalendarContract.Events.CONTENT_URI

    selectedDays.forEach { dayOfWeek ->

        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val values = ContentValues().apply {
            put(
                CalendarContract.Events.CALENDAR_ID,
                calendarId
            )
            put(CalendarContract.Events.TITLE, "AirBeats Practice")
            put(CalendarContract.Events.DTSTART, calendar.timeInMillis) // Start Time
            put(CalendarContract.Events.DTEND, calendar.timeInMillis + 3600000) // 1 hour duration
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

            val weekday = when (dayOfWeek) {
                Calendar.SUNDAY -> "SU"
                Calendar.MONDAY -> "MO"
                Calendar.TUESDAY -> "TU"
                Calendar.WEDNESDAY -> "WE"
                Calendar.THURSDAY -> "TH"
                Calendar.FRIDAY -> "FR"
                Calendar.SATURDAY -> "SA"
                else -> throw IllegalArgumentException("Invalid day of week")
            }
            put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$weekday")
        }
        calendarList.add(calendar)
        contentResolver.insert(calendarUri, values)

    }
}

fun checkAirBeatsPracticeEvents(context: Context): HashMap<String, MutableList<Event>> {
    val eventList = mutableListOf<Event>()

    val contentResolver: ContentResolver = context.contentResolver
    val calendarId = getPrimaryCalendarId(contentResolver) // Your function to get the calendar ID

    val calendar = Calendar.getInstance()
    calendar.firstDayOfWeek = Calendar.MONDAY
    calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)

    val startMillis = calendar.timeInMillis

    calendar.add(Calendar.WEEK_OF_YEAR, 1) // Get events for 1 full week
    val endMillis = calendar.timeInMillis

    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        .appendPath(startMillis.toString())
        .appendPath(endMillis.toString())
        .build()

    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY
    )

    val selection = "${CalendarContract.Instances.CALENDAR_ID} = ?"
    val selectionArgs = arrayOf(calendarId.toString())

    val cursor: Cursor? = contentResolver.query(
        uri,
        projection,
        selection,
        selectionArgs,
        "${CalendarContract.Instances.BEGIN} ASC"
    )

    cursor?.use {
        val idIndex = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
        val titleIndex = it.getColumnIndex(CalendarContract.Instances.TITLE)
        val beginIndex = it.getColumnIndex(CalendarContract.Instances.BEGIN)
        val endIndex = it.getColumnIndex(CalendarContract.Instances.END)
        val allDayIndex = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)

        while (it.moveToNext()) {
            val eventId = it.getLong(idIndex)
            val eventTitle = it.getString(titleIndex)
            val eventStartTime = it.getLong(beginIndex)
            val eventEndTime = it.getLong(endIndex)
            val isAllDay = it.getInt(allDayIndex) == 1

            val calendar = Calendar.getInstance().apply { timeInMillis = eventStartTime }
            val dayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.time)
            val dayShortcut = SimpleDateFormat("EEE", Locale.ENGLISH).format(calendar.time)
            val startTime =
                if (isAllDay) "All Day" else SimpleDateFormat("HH:mm", Locale.ENGLISH).format(
                    calendar.time
                )

            eventList.add(
                Event(
                    eventId,
                    eventTitle,
                    dayName,
                    startTime,
                    dayShortcut,
                    eventStartTime,
                    eventEndTime
                )
            )
        }
    }

    val events = HashMap<String, MutableList<Event>>()
    eventList.forEach { event ->
        if (events[event.startTime] == null) {
            events[event.startTime] = mutableListOf()
        }
        events[event.startTime]?.add(event)
    }

    return events
}


fun updateAirBeatsEvents(
    context: Context,
    events: List<Event>,
    daysToDelete: List<String>,
    daysToAdd: List<String>,
    selectedHour: Int,
    selectedMinute: Int
) {
    val daysOfWeek = hashMapOf(
        "Sunday" to Calendar.SUNDAY,
        "Monday" to Calendar.MONDAY,
        "Tuesday" to Calendar.TUESDAY,
        "Wednesday" to Calendar.WEDNESDAY,
        "Thursday" to Calendar.THURSDAY,
        "Friday" to Calendar.FRIDAY,
        "Saturday" to Calendar.SATURDAY,
    )
    val contentResolver = context.contentResolver
    val calendarId = getPrimaryCalendarId(contentResolver)

    // Validate newStartTime format (HH:mm)

    // Remove events belonging to days in daysToDelete from the calendar
    events.filter { it.dayName in daysToDelete }.forEach { event ->
        contentResolver.delete(
            CalendarContract.Events.CONTENT_URI,
            "${CalendarContract.Events._ID} = ?",
            arrayOf(event.eventId.toString())
        )
    }

    // Update startTime for remaining events in the list
    val filteredEvents = events.filterNot { it.dayName in daysToDelete }
    for (event in filteredEvents) {
        val dayOfWeek = daysOfWeek.get(event.dayName)!!

        val weekday = when (dayOfWeek) {
            Calendar.SUNDAY -> "SU"
            Calendar.MONDAY -> "MO"
            Calendar.TUESDAY -> "TU"
            Calendar.WEDNESDAY -> "WE"
            Calendar.THURSDAY -> "TH"
            Calendar.FRIDAY -> "FR"
            Calendar.SATURDAY -> "SA"
            else -> throw IllegalArgumentException("Invalid day of week")
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, dayOfWeek)
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val values = ContentValues().apply {
            put(
                CalendarContract.Events.CALENDAR_ID,
                calendarId
            )
            put(CalendarContract.Events.TITLE, "AirBeats Practice")
            put(CalendarContract.Events.DTSTART, calendar.timeInMillis)
            put(CalendarContract.Events.DTEND, calendar.timeInMillis + 3600000)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)

            put(CalendarContract.Events.RRULE, "FREQ=WEEKLY;BYDAY=$weekday")
        }
        contentResolver.update(
            CalendarContract.Events.CONTENT_URI,
            values,
            "${CalendarContract.Events._ID} = ?",
            arrayOf(event.eventId.toString())
        )
    }

    if (daysToAdd.isNotEmpty()) {

        val daysToAddCodes = daysToAdd.map { daysOfWeek.get(it)!! }.toSet()
        createCalendarForRecurringEvent(
            context,
            daysToAddCodes,
            selectedHour,
            selectedMinute
        )
    }

}

// Helper function to convert day shortcut (SU, MO, etc.) to Calendar constant
fun getDayOfWeekFromShortcut(shortcut: String): Int {
    return when (shortcut) {
        "SU" -> Calendar.SUNDAY
        "MO" -> Calendar.MONDAY
        "TU" -> Calendar.TUESDAY
        "WE" -> Calendar.WEDNESDAY
        "TH" -> Calendar.THURSDAY
        "FR" -> Calendar.FRIDAY
        "SA" -> Calendar.SATURDAY
        else -> throw IllegalArgumentException("Invalid day shortcut")
    }
}
