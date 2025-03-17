package pl.put.airbeats

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import pl.put.airbeats.ui.LoginScreen
import pl.put.airbeats.ui.theme.AirBeatsTheme

@Composable
fun AirBeatsApp() {
    var currentScreen by rememberSaveable { mutableStateOf("Login") }
    var userToken by rememberSaveable { mutableStateOf("") }
    if(userToken == "") {
        currentScreen = "Login"
    }else{
        currentScreen = "Main"
    }
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when(currentScreen) {
            "Login" -> LoginScreen(userToken
                , { userToken = it }
                , Modifier.padding(innerPadding)
            )
            "Main" -> Text("Main Screen for user:$userToken"
                , modifier = Modifier.padding(innerPadding)
            )
            else -> LoginScreen(userToken
                , { userToken = it }
                , Modifier.padding(innerPadding)
            )
        }
    }
}

@Preview(showBackground = true
, showSystemUi = true
)
@Composable
fun AirBeatsAppPreview() {
    AirBeatsTheme {
        AirBeatsApp()
    }
}