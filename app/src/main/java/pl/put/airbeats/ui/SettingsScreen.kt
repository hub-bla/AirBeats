package pl.put.airbeats.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pl.put.airbeats.LocalUser
import pl.put.airbeats.routes.Screen

@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    Scaffold { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LogoutButton(navController)
        }
    }
}

@Composable
fun LogoutButton(navController: NavController) {
    val userState = LocalUser.current
    fun logoutUser() {
        Firebase.auth.signOut()
        userState.value = ""
        Log.d("LogoutButton", userState.value)
        navController.navigate(Screen.Login.route)
    }
    Button(onClick = ::logoutUser) {
        Text("Log out")
    }
}