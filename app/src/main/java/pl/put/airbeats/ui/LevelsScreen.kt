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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun LevelsScreen(modifier: Modifier = Modifier) {
    // variable that after being changes through button it displays correct songs from the levels
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
                onClick = {onDifficultySelected("medium") }) {
                Text("Medium")
            }

            Button(
                modifier = Modifier.fillMaxWidth(0.6f),
                onClick = {onDifficultySelected("hard")}) {
                Text("Hard")
            }
        }

    }
}

@Composable
fun SongsInDifficulty(difficulty: MutableState<String>, modifier: Modifier = Modifier) {
    // need to fetch data from firestore
    Text("Songs here")
}