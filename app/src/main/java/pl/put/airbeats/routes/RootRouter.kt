package pl.put.airbeats.routes

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.navigation.NavType

import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pl.put.airbeats.LocalUser
import pl.put.airbeats.ui.GameScreen
import pl.put.airbeats.ui.HomeScreen
import pl.put.airbeats.ui.LevelsScreen
import pl.put.airbeats.ui.LoginScreen
import pl.put.airbeats.ui.RemindersScreen
import pl.put.airbeats.ui.SettingsScreen
import pl.put.airbeats.ui.StatisticsScreen
import pl.put.airbeats.utils.room.AirBeatsViewModel

@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
@Composable
fun RootRouter(airBeatsViewModel: AirBeatsViewModel) {
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
            LevelsScreen(navController)
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController, airBeatsViewModel)
        }

        composable(
            route = "${Screen.Game.route}/{songName}/{difficulty}",
            arguments = listOf(
                navArgument("songName") { type = NavType.StringType },
                navArgument("difficulty") { type = NavType.StringType }
            )) { backStackEntry ->
            val songName: String = backStackEntry.arguments?.getString("songName") ?: ""
            val difficulty: String = backStackEntry.arguments?.getString("difficulty") ?: ""
            GameScreen(songName, difficulty, airBeatsViewModel)
        }

        composable(route = Screen.Settings.route + "/reminders") {
            RemindersScreen(navController)
        }

        composable(route = Screen.Statistics.route) {
            StatisticsScreen(airBeatsViewModel)
        }
    }
}