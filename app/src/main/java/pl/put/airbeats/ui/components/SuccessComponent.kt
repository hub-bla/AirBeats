package pl.put.airbeats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SuccessComponent(
    text: String,
    timeInMs: Int,
    modifier: Modifier = Modifier
) {
    var barWidth by remember { mutableDoubleStateOf(1.0) }

    LaunchedEffect(Unit) {
        val delayTimeMs = (timeInMs.toDouble() / 100.0).toLong()
        while (barWidth > 0) {
            delay(delayTimeMs)
            barWidth -= 0.01
        }

    }
    if (barWidth > 0) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.7f)
                .heightIn(min = 75.dp)
                .background(Color(0xff02a026), shape = RoundedCornerShape(16.dp))
        ) {
            Text(
                text = text, color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
            )
            TimeBar(barWidth, modifier = Modifier.align(Alignment.BottomStart))
        }
    }
}

@Composable
fun TimeBar(barWidth: Double, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(5.dp)
            .fillMaxWidth(barWidth.toFloat())
            .background(Color(0xffedeeed), shape = RoundedCornerShape(16.dp))
            .graphicsLayer(alpha = 0.5f)
    ) {

    }
}