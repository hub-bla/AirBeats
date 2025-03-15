package pl.put.airbeats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pl.put.airbeats.ui.theme.AirBeatsTheme



@Composable
fun Login() {
    Column(modifier = Modifier
        .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Login")
//        LoginInput
        Text("Password")
//        PasswordInput
//        LoginButton
//        RegisterButton
    }
}

@Preview(
    name = "Login Screen Preview",
    showBackground = true,
    showSystemUi = true)
@Composable
fun PreviewLogin() {
    AirBeatsTheme {
        Login()
    }
}