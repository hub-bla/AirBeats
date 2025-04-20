package pl.put.airbeats.ui

import android.Manifest
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import pl.put.airbeats.ui.components.CalendarEventComponent
import pl.put.airbeats.ui.components.CalendarForm
import pl.put.airbeats.utils.Event
import pl.put.airbeats.utils.checkAirBeatsPracticeEvents

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

    LaunchedEffect(writePermissionState.status.isGranted, readPermissionState.status.isGranted) {
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
    }

    LaunchedEffect(isGranted) {
        if (isGranted) {
            events = checkAirBeatsPracticeEvents(context)
        }
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