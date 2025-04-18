package pl.put.airbeats

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import pl.put.airbeats.routes.RootRouter
import pl.put.airbeats.ui.theme.AirBeatsTheme
import pl.put.airbeats.utils.midi.MidiReader


val LocalUser = compositionLocalOf<MutableState<String>> { mutableStateOf("") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            AirBeatsTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) {
                val userState = remember { mutableStateOf("") }

                CompositionLocalProvider(LocalUser provides userState) {
                    LocalUser.current.value = Firebase.auth.currentUser?.uid.toString()
                    RootRouter()
                }
//                    AirBeatsApp(Firebase.auth)
//                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AirBeatsTheme {
        Greeting("Android")
    }
}