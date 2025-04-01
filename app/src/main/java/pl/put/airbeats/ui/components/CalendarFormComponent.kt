package pl.put.airbeats.ui.components

import android.app.TimePickerDialog
import android.content.Context
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import pl.put.airbeats.utils.Event
import pl.put.airbeats.utils.createCalendarForRecurringEvent
import pl.put.airbeats.utils.updateAirBeatsEvents
import java.util.Calendar
import kotlin.collections.component1
import kotlin.collections.component2

// TODO: Invalidate adding event to calendar that already exist at specific time and day
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
        if (time == "None") {
            val calendar = Calendar.getInstance()
            selectedHour = calendar.get(Calendar.HOUR_OF_DAY)
            selectedMinute = calendar.get(Calendar.MINUTE)
            return@LaunchedEffect
        }
        val (hours, minutes) = time.split(":")
        selectedHour = hours.toInt()
        selectedMinute = minutes.toInt()
    }

    LaunchedEffect(selectedDays) {
        val eventsDayNames = events.map { it.dayName }.toSet().toList()
        val stringSelectedDays = selectedDays.map { daysOfWeekReversed.getOrDefault(it, "") }

        daysToDelete = eventsDayNames - stringSelectedDays
        daysToAdd = stringSelectedDays - eventsDayNames
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
            val sortedDays = daysOfWeek.entries.sortedBy {
                when (it.value) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 7
                }
            }
            sortedDays.forEachIndexed { index, (dayName, calendarDay) ->
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
                Text("Add daily practice")
            }
        } else {
            Button(onClick = {
                updateAirBeatsEvents(
                    context,
                    events,
                    daysToDelete,
                    daysToAdd,
                    selectedHour,
                    selectedMinute
                )
                savedStateHandle?.set(
                    "successMessage",
                    "Successfully updated daily practice in calendar."
                )

                navController.popBackStack()

            }, enabled = !isSuccess && selectedDays.isNotEmpty()) {
                Text("Update daily practice")
            }
            Button(onClick = {
                updateAirBeatsEvents(
                    context,
                    events,
                    events.map { it.dayName }.toSet().toList(),
                    emptyList(),
                    selectedHour,
                    selectedMinute
                )
                savedStateHandle?.set(
                    "successMessage",
                    "Successfully deleted daily practice from calendar."
                )

                navController.popBackStack()

            }) {
                Text("Delete daily practice")
            }
        }

    }
}