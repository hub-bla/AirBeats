package pl.put.airbeats.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ErrorComponent(message: String, modifier: Modifier = Modifier) {
    Text(text = message, color = Color.Red, modifier = modifier)
}