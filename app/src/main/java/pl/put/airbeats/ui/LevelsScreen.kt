package pl.put.airbeats.ui

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
    val audioLink: String
)

@Composable
fun SongsInDifficulty(difficulty: MutableState<String>, modifier: Modifier = Modifier) {
    var songs = remember { mutableStateOf<List<SongData>>(emptyList()) }
    LaunchedEffect(difficulty.value) {
        val db = Firebase.firestore

        db.collection("${difficulty.value}_songs")
            .get()
            .addOnSuccessListener { result ->
                val fetchedSongs = result.map { document ->
                    val songName = document.id
                    val midiLink = document.getString("midi").toString()
                    val audioLink = document.getString("audio").toString()

                    SongData(songName, midiLink, audioLink)
                }

                songs.value = fetchedSongs
            }

    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (songs.value.isEmpty()) {
            LoadingScreen()
            return
        } else {
            songs.value.forEach { song ->
                Song(song.songName, song.midiLink, song.audioLink)
            }
        }

    }
}

@Composable
fun Song(songName: String, midiLink: String, audioLink: String) {

    Button(onClick = {}) { // pass midiLink and audioLink to the actual game screen
        Text(songName)
    }

}