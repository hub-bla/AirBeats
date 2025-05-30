package pl.put.airbeats.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pl.put.airbeats.LocalUser
import pl.put.airbeats.routes.Screen
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import pl.put.airbeats.ui.components.SuccessComponent
import pl.put.airbeats.utils.room.AirBeatsViewModel

@Composable
fun SettingsScreen(
    navController: NavController,
    airBeatsViewModel: AirBeatsViewModel,
    modifier: Modifier = Modifier
) {
    val backStackEntry = navController.currentBackStackEntry
    val savedStateHandle = backStackEntry?.savedStateHandle
    var successMessage by remember { mutableStateOf("") }
    val isSavingEnergy by airBeatsViewModel.isSavingEnergy.collectAsState()

    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.get<String>("successMessage")?.let {
            savedStateHandle["successMessage"] = ""
            successMessage = it
        }

    }

    val userState = LocalUser.current
    fun logoutUser() {
        Firebase.auth.signOut()
        userState.value = ""
        Log.d("LogoutButton", userState.value)
        successMessage = ""
        navController.navigate(Screen.Login.route)
    }

    fun moveToCalendarScreen() {
        successMessage = ""
        navController.navigate(Screen.Settings.route + "/reminders")
    }
    Scaffold { paddingValues ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            if (successMessage != "") {
                SuccessComponent(
                    successMessage,
                    3000,
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 30.dp)
                )
            }
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                Button(onClick = ::moveToCalendarScreen) {
                    Text("Calendar Reminders")
                }

                Button(onClick = ::logoutUser) {
                    Text("Log out")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = isSavingEnergy,
                        onCheckedChange = { airBeatsViewModel.changeSavingMode(it) }
                    )
                    Text("saving energy mode")
                }
            }
        }
    }

}