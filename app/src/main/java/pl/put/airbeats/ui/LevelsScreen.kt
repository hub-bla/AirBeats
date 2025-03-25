package pl.put.airbeats.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import pl.put.airbeats.ui.components.ErrorComponent
import pl.put.airbeats.ui.components.Loading
import pl.put.airbeats.utils.midi.MidiReader

@Composable
fun LevelsScreen(modifier: Modifier = Modifier) {
    var difficulty = remember { mutableStateOf("") }

    when (difficulty.value) {
        "" -> SelectDifficulty(onDifficultySelected = { newDifficulty ->
            difficulty.value = newDifficulty
        })

        else -> SongsInDifficulty(difficulty)
    }

}


@Composable
fun SelectDifficulty(onDifficultySelected: (String) -> Unit, modifier: Modifier = Modifier) {
    Scaffold { paddingValues ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Select difficulty")
            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = { onDifficultySelected("easy") }) {
                Text("Easy")
            }

            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = { onDifficultySelected("medium") }) {
                Text("Medium")
            }

            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = { onDifficultySelected("hard") }) {
                Text("Hard")
            }
        }

    }
}

data class SongData(
    val songName: String,
    val midiLink: String,
    val audioLink: String,
    val bpm: Int
)

@Composable
fun SongsInDifficulty(difficulty: MutableState<String>, modifier: Modifier = Modifier) {
    var songs = remember { mutableStateOf<List<SongData>>(emptyList()) }
    var isLoading = remember { mutableStateOf(true) }
    var error = remember { mutableStateOf("") }
    LaunchedEffect(difficulty.value) {
        val db = Firebase.firestore

        db.collection("${difficulty.value}_songs")
            .get()
            .addOnSuccessListener { result ->
                val fetchedSongs = result.map { document ->
                    val songName = document.id
                    val midiLink = document.getString("midi").toString()
                    val audioLink = document.getString("audio").toString()
                    val bpm = document.getLong("bpm")!!.toInt()

                    SongData(songName, midiLink, audioLink, bpm)
                }

                songs.value = fetchedSongs
                isLoading.value = false
            }.addOnFailureListener {
                Log.d("Firestore failure", "couldn't fetch data")
                isLoading.value = false
                error.value = "Couldn't fetch songs data."
            }

    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading.value) {
            Loading()
            return
        }

        if (error.value != "") {
            ErrorComponent(error.value)
        }

        if (songs.value.isEmpty()) {
            Text("No songs were found")
        }

        songs.value.forEach { song ->
            Song(song.songName, song.midiLink, song.audioLink, song.bpm)
        }

    }
}

@Composable
fun Song(songName: String, midiLink: String, audioLink: String, bpm: Int) {
    Button(onClick = {
        runBlocking {
            val midi = MidiReader()
            midi.read(midiLink, bpm)
        }

    }) { // pass midiLink and audioLink to the actual game screen
        Text(songName)
    }

}