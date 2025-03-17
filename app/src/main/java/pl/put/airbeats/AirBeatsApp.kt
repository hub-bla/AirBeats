package pl.put.airbeats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import pl.put.airbeats.ui.LoginScreen
import pl.put.airbeats.ui.theme.AirBeatsTheme

@Composable
fun AirBeatsApp(auth: FirebaseAuth) {
    var currentScreen by rememberSaveable { mutableStateOf("Login") }
    var currentUserUID by rememberSaveable { mutableStateOf("") }

    currentUserUID = auth.currentUser?.uid ?: ""
    currentScreen = if(currentUserUID == "") "Login" else "Main"

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when(currentScreen) {
            "Login" -> LoginScreen(auth
                , {currentUserUID = it}
                , Modifier.padding(innerPadding)
            )
            "Main" -> Column (modifier = Modifier.padding(innerPadding)) {
                Text("Main Screen for user:$currentUserUID")
                Button(onClick = {auth.signOut();currentUserUID = ""}) {
                    Text("Logout")
                }
            }
            else -> Text("Error 404 Screen not found"
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
        AirBeatsApp(auth = Firebase.auth)
    }
}