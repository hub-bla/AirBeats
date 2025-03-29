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
import pl.put.airbeats.ui.*


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
            LevelsScreen(navController)
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController)
        }
//        composable(route = "${Screen.Game.route}/{songName}/{midiLink}/{audioLink}/{bpm}",
        composable(route = "${Screen.Game.route}/{songName}/{difficulty}",
            arguments = listOf(
                navArgument("songName") {type = NavType.StringType},
                navArgument("difficulty") {type = NavType.StringType},
//                navArgument("midiLink") {type = NavType.StringType},
//                navArgument("audioLink") {type = NavType.StringType},
//                navArgument("bpm") {type = NavType.IntType}
            )){ backStackEntry ->
            val songName: String = backStackEntry.arguments?.getString("songName") ?: ""
            val difficulty: String = backStackEntry.arguments?.getString("difficulty") ?: ""
//            val midiLink: String = backStackEntry.arguments?.getString("midiLink") ?: ""
//            val audioLink: String = backStackEntry.arguments?.getString("audioLink") ?: ""
//            val bpm: Int = backStackEntry.arguments?.getInt("bpm") ?: 0
//            GameScreen(songName, midiLink, audioLink, bpm)
            GameScreen(songName, difficulty)
        }
    }
}