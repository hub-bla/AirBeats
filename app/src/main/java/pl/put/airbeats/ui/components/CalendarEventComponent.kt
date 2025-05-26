package pl.put.airbeats.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import pl.put.airbeats.utils.Event
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalendarEventComponent(time: String, events: List<Event>, onClick: () -> Unit) {
    val sortedDays = events
        .map { it.dayShortcut }
        .toSet()
        .sortedWith(Comparator { day1, day2 ->
            val order = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            order.indexOf(day1).compareTo(order.indexOf(day2))
        })

    Column(
        modifier = Modifier
            .fillMaxWidth(0.7f)
            .background(
                color = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
            .shadow(
                0.dp,
                shape = RoundedCornerShape(8.dp),
                ambientColor = Color.Black,
                spotColor = Color.Gray
            )
            .padding(20.dp).clickable{
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind() {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, size.height), // Starting at the bottom-left
                        end = Offset(size.width, size.height), // Ending at the bottom-right
                        strokeWidth = 1f // Border thickness
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            sortedDays.forEach { dayName ->
                Log.d("Component", dayName)
                Text(
                    text = dayName,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Text(
            time,
            fontSize = 42.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
    }
}