package pl.put.airbeats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import pl.put.airbeats.LocalUser
import pl.put.airbeats.routes.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pl.put.airbeats.utils.streak.StreakData
import pl.put.airbeats.utils.streak.getStreak
import pl.put.airbeats.utils.LottieLoading

@Composable
fun HomeScreen(navController: NavController, modifier: Modifier = Modifier) {
    val userState = LocalUser.current
    val streak = getStreak(LocalContext.current) ?: StreakData(0, "0000-00-00")
    if (userState.value == "") {
        navController.navigate(Screen.Login.route)
    }
    Scaffold { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Streak: " + streak.streak.toString())
            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = {navController.navigate(Screen.Levels.route)}
            ) {
                Text("Play")
            }

            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = {navController.navigate(Screen.Statistics.route) }
            ) {
                Text("Game History")
            }

            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = {navController.navigate(Screen.Settings.route)}
            ) {
                Text("Settings")
            }
            LottieLoading(size = 0.dp)
        }

    }


}