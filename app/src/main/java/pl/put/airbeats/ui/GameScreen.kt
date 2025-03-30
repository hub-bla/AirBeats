package pl.put.airbeats.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import pl.put.airbeats.ui.components.ErrorComponent
import pl.put.airbeats.ui.components.Loading
import pl.put.airbeats.utils.game.MyGLSurfaceView
import pl.put.airbeats.utils.midi.MidiReader
import pl.put.airbeats.utils.midi.NoteTrack

@Composable
fun GameScreen(songName: String, difficulty: String, modifier: Modifier = Modifier) {
    var noteTracks = remember { mutableStateOf(emptyMap<String, NoteTrack>()) }
    var audioLink = remember { mutableStateOf("") }
    var isPlaying = remember { mutableStateOf(false) }

    when(isPlaying.value) {
        false -> Menu(
            {newNoteTracks ->  noteTracks.value = newNoteTracks},
            {newAudioLink ->  audioLink.value = newAudioLink},
            songName,
            difficulty,
            {isPlaying.value = true},
            modifier)

        true -> Game(audioLink.value, noteTracks.value)
    }
}

@Composable
fun Menu(changeNoteTracks: (Map<String, NoteTrack>) -> Unit, changeAudioLink: (String) -> Unit, songName: String, difficulty: String, startGame: () -> Unit, modifier: Modifier = Modifier) {
    var isLoading = remember { mutableStateOf(true) }
    var error = remember { mutableStateOf("") }

    LaunchedEffect(difficulty, songName) {
        Log.d("Game", "LaunchedEffect")
        val db = Firebase.firestore
        var midiLink = ""
        var audioLink = ""
        var bpm = 0

        db.collection("${difficulty}_songs").document(songName)
            .get()
            .addOnSuccessListener { result ->
                Log.d("Firestore success", "Song document loaded")
                midiLink = result.get("midi").toString()
                audioLink = result.get("audio").toString()
                bpm = result.get("bpm", ).toString().toInt()

                if(midiLink == "") {
                    Log.d("Game", "Song document is empty")
                    isLoading.value = false
                    error.value = "Song data is not available."
                    return@addOnSuccessListener
                }

                runBlocking {
                    val midi = MidiReader()
                    val noteTracks = midi.read(midiLink, bpm)
                    Log.d("Game", "Note Tracks loaded")
                    changeAudioLink(audioLink)
                    changeNoteTracks(noteTracks)
                }
                isLoading.value = false
            }.addOnFailureListener {
                Log.d("Firestore failure", "couldn't fetch data")
                isLoading.value = false
                error.value = "Couldn't fetch song data."
            }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(songName)

        if (isLoading.value) {
            Loading()
            return
        }

        if (error.value != "") {
            ErrorComponent(error.value)
        }

        Button(onClick = startGame, enabled = (error.value == "")) {
            Text("play")
        }
    }
}

@Composable
fun Game(audioLink: String, noteTrack: Map<String, NoteTrack>, modifier: Modifier = Modifier) {
//        TODO game mechanic
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {context ->
             MyGLSurfaceView(context)
        }
    )
}