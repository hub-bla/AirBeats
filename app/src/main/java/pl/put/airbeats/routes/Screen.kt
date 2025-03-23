package pl.put.airbeats.routes

sealed class Screen(val route: String) {
    object Main : Screen("home")
    object Login : Screen("login")
}