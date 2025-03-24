package pl.put.airbeats.routes

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pl.put.airbeats.LocalUser
import pl.put.airbeats.ui.HomeScreen
import pl.put.airbeats.ui.LevelsScreen
import pl.put.airbeats.ui.LoginScreen
import pl.put.airbeats.ui.SettingsScreen

@Composable
fun RootRouter() {
    val navController = rememberNavController()
    val userState = LocalUser.current
    val isLoggedIn = Firebase.auth.currentUser?.uid == null || userState.value == ""

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Login.route else Screen.Main.route
    ) {
        composable(route = Screen.Main.route) {
            HomeScreen(navController, Modifier)
        }
        composable(route = Screen.Login.route) {
            LoginScreen(Firebase.auth, navController)
        }
        composable(route = Screen.Levels.route) {
            LevelsScreen()
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController)
        }
    }
}