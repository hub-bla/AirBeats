package pl.put.airbeats.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import androidx.compose.runtime.*
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import pl.put.airbeats.ui.components.CalendarEventComponent
import pl.put.airbeats.utils.Event
import pl.put.airbeats.utils.checkAirBeatsPracticeEvents
import pl.put.airbeats.utils.getPrimaryCalendarId
import pl.put.airbeats.utils.updateAirBeatsEvents
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.associate

//import pl.put.airbeats.Manifest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CalendarPermissionRequest(permissionState: PermissionState, onPermissionGranted: () -> Unit) {
    LaunchedEffect(permissionState) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        } else {
            onPermissionGranted()
        }
    }
}

// TODO: Add info that adding to calendar was sucessful

// Maybe a new universal component that would have section
// top bar and content
// and in top bar this information that something was successful could be displayed
@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun RemindersScreen(navController: NavController) {
    val writePermissionState = rememberPermissionState(Manifest.permission.WRITE_CALENDAR)
    val readPermissionState = rememberPermissionState(Manifest.permission.READ_CALENDAR)
    var isGranted by remember {
        mutableStateOf(
            writePermissionState.status.isGranted
                    && readPermissionState.status.isGranted
        )
    }

    var events by remember { mutableStateOf(HashMap<String, MutableList<Event>>()) }

    val context = LocalContext.current
    val backStackEntry = navController.previousBackStackEntry
    val savedStateHandle = backStackEntry?.savedStateHandle

    var screenState by remember { mutableStateOf("calendarEvents") }
    var pickedTime by remember { mutableStateOf("None") }



    LaunchedEffect(writePermissionState, readPermissionState) {
        if (!writePermissionState.status.isGranted) {
            writePermissionState.launchPermissionRequest()
        } else {
            isGranted = true && readPermissionState.status.isGranted
        }

        if (!readPermissionState.status.isGranted) {
            readPermissionState.launchPermissionRequest()
        } else {
            isGranted = true && writePermissionState.status.isGranted
        }
        events = checkAirBeatsPracticeEvents(context)
    }

    LaunchedEffect(Unit) {
        events = checkAirBeatsPracticeEvents(context)
    }

    if (isGranted) {
        when (screenState) {
            "calendarEvents" -> {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(onClick = {
                        pickedTime = "None"
                        screenState = "calendarEventsCreator"
                    }) {
                        Text("Create new calendar reminder")
                    }
                    events.entries.forEachIndexed { index, (time, events) ->
                        CalendarEventComponent(time, events, {
                            pickedTime = time
                            screenState = "calendarEventsCreator"
                        })
                        if (index < events.size - 1) { // Add spacer only between items
                            Spacer(modifier = Modifier.height(16.dp)) // Adjust the height to your preference
                        }
                    }

                }
            }

            "calendarEventsCreator" -> {
                val calendarEventsAtTime = events.getOrDefault(pickedTime, emptyList<Event>())
                CalendarForm(
                    navController,
                    context,
                    savedStateHandle,
                    pickedTime,
                    calendarEventsAtTime
                )
            }
        }

    }
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

    Log.d("send Days", selectedDays.toString())

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
            ) // Default calendar ID (Ensure the correct ID)
            put(CalendarContract.Events.TITLE, "AirBeats Practice")
//            put(CalendarContract.Events.DESCRIPTION, "Recurring event every Monday at 10 PM")
//            put(CalendarContract.Events.EVENT_LOCATION, "Office or Online")
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarForm(
    navController: NavController,
    context: Context,
    savedStateHandle: SavedStateHandle?,
    time: String = "10:00",
    events: List<Event> = emptyList<Event>()
) {
    var isSuccess by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var selectedHour by remember { mutableIntStateOf(0) } // Default to 10 AM
    var selectedMinute by remember { mutableIntStateOf(0) } // Default to 00 minutes
    var daysToDelete by remember { mutableStateOf(emptyList<String>()) }
    var daysToAdd by remember { mutableStateOf(emptyList<String>()) }

    var showTimePickerDialog by remember { mutableStateOf(false) }

    val daysOfWeek = hashMapOf(
        "Sunday" to Calendar.SUNDAY,
        "Monday" to Calendar.MONDAY,
        "Tuesday" to Calendar.TUESDAY,
        "Wednesday" to Calendar.WEDNESDAY,
        "Thursday" to Calendar.THURSDAY,
        "Friday" to Calendar.FRIDAY,
        "Saturday" to Calendar.SATURDAY,
    )

    val daysOfWeekReversed = daysOfWeek.entries.associate { (key, value) -> value to key }

    LaunchedEffect(Unit) {
        selectedDays = events.mapNotNull { daysOfWeek[it.dayName] }.toSet()
        Log.d("Seldays", events.toString())
        if (time == "None") {
            return@LaunchedEffect
        }
        val (hours, minutes) = time.split(":")
        selectedHour = hours.toInt()
        selectedMinute = minutes.toInt()
    }

    LaunchedEffect(selectedDays) {
        // compare events with selectedDays

        // days that are not in events should be added to daysToAdd
        // days that are not in selectedDays should be added to daysToDelete
        val eventsDayNames = events.map { it.dayName }.toSet().toList()
        val stringSelectedDays = selectedDays.map { daysOfWeekReversed.getOrDefault(it, "") }

        daysToDelete = eventsDayNames - stringSelectedDays
        daysToAdd = stringSelectedDays - eventsDayNames
        Log.d("Bonjour", daysToDelete.toString())
        Log.d("Bonjour2", daysToAdd.toString())
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .padding(top = 20.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isSuccess) {
            Text(text = "Success!", color = Color.Green)
        }

        Text(text = "Select Recurring Event")

        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalArrangement = Arrangement.Center,
        ) {
            daysOfWeek.entries.forEachIndexed { index, (dayName, calendarDay) ->
                Row(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = dayName,
                        textAlign = TextAlign.Center,
                        color = (if (selectedDays.contains(calendarDay)) Color.White else Color.Black),
                        modifier = Modifier
                            .background(
                                if (selectedDays.contains(calendarDay)) Color.Blue else Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                val newSelectedDays = if (selectedDays.contains(calendarDay)) {
                                    selectedDays - calendarDay
                                } else {
                                    selectedDays + calendarDay
                                }
                                selectedDays = newSelectedDays
                            }
                            .padding(20.dp)
                            .widthIn(min = 100.dp)
                    )
                }
            }
        }


        Spacer(modifier = Modifier.height(16.dp))

        // Show selected days and time
        Text("Selected Days:")
        Text(
            text = selectedDays.joinToString { daysOfWeekReversed.getOrDefault(it, "") },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
        Text("Selected Time: $selectedHour:${String.format("%02d", selectedMinute)}")

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showTimePickerDialog = true }) {
            Text("Pick Time")
        }

        // TimePickerDialog
        if (showTimePickerDialog) {
            TimePickerDialog(
                LocalContext.current,
                { _, hourOfDay, minute ->
                    selectedHour = hourOfDay
                    selectedMinute = minute
                    showTimePickerDialog = false
                },
                selectedHour,
                selectedMinute,
                false
            ).show()
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (time == "None") {
            Button(onClick = {
                if (!isSuccess) {
                    isSuccess = true
                    createCalendarForRecurringEvent(
                        context,
                        selectedDays,
                        selectedHour,
                        selectedMinute
                    )
                    savedStateHandle?.set(
                        "successMessage",
                        "Successfully added daily practice to calendar."
                    )

                    navController.popBackStack()
                }

            }, enabled = !isSuccess && selectedDays.isNotEmpty()) {
                Text("Confirm Recurring Event")
            }
        } else {
            Button(onClick = {
                // create function for modification of events
                updateAirBeatsEvents(
                    context,
                    events,
                    daysToDelete,
                    daysToAdd,
                    "$selectedHour:${String.format("%02d", selectedMinute)}"
                )
                savedStateHandle?.set(
                    "successMessage",
                    "Successfully updated daily practice to calendar."
                )

                navController.popBackStack()

            }, enabled = !isSuccess && selectedDays.isNotEmpty()) {
                Text("Update Recurring Event")
            }
            Button(onClick = {
                updateAirBeatsEvents(
                    context,
                    events,
                    events.map{it.dayName}.toSet().toList(),
                    emptyList(),
                    "$selectedHour:${String.format("%02d", selectedMinute)}"
                )
                savedStateHandle?.set(
                    "successMessage",
                    "Successfully deleted daily practice from calendar."
                )

                navController.popBackStack()

            }) {
                Text("Delete Recurring Event")
            }
        }

    }
}