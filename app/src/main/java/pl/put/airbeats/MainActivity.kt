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
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import pl.put.airbeats.utils.room.LevelStatisticDatabase
import pl.put.airbeats.utils.room.LevelStatisticViewModel


val LocalUser = compositionLocalOf<MutableState<String>> { mutableStateOf("") }

class MainActivity : ComponentActivity() {
    val db by lazy {
        Room.databaseBuilder(
            applicationContext,
            LevelStatisticDatabase::class.java,
            "AirBeats-database"
        ).build()
    }
    val levelStatisticviewModel: LevelStatisticViewModel by viewModels<LevelStatisticViewModel>(
        factoryProducer = {
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    @Suppress("UNCHECKED_CAST")
                    return LevelStatisticViewModel(db.dao()) as T
                }
            }
        }
    )
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                1001
            )
        }

        setContent {
            AirBeatsTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) {
                val userState = remember { mutableStateOf("") }

                CompositionLocalProvider(LocalUser provides userState) {
                    LocalUser.current.value = Firebase.auth.currentUser?.uid.toString()
                    RootRouter(levelStatisticviewModel)
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
