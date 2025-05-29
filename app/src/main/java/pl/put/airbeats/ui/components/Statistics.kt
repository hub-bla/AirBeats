package pl.put.airbeats.ui.components

import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.put.airbeats.ui.theme.AirBeatsTheme
import pl.put.airbeats.utils.room.LevelStatisticEntity

@Composable
fun Statistics(
    levelStatistic:  LevelStatisticEntity,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .width(350.dp)
            .clickable(onClick = {isExpanded = !isExpanded})
    ) {
        Column (
            Modifier
                .fillMaxWidth()
                .padding(15.dp)
        ) {
            Row (
                Modifier
                    .fillMaxWidth(),
            ){
                Chip("Level:", levelStatistic.songName, Modifier.weight(1f))
                Chip("Difficulty:", levelStatistic.difficulty, Modifier.weight(1f))
            }
            Row (
                Modifier
                    .fillMaxWidth(),
            ){
                Chip("Points:", levelStatistic.points.toString(), Modifier.weight(1f))
                Chip("Date:", levelStatistic.date, Modifier.weight(1f))
            }
            HorizontalDivider()
            if(isExpanded){
                Chip("Max Combo:", "${levelStatistic.maxCombo}", Modifier.fillMaxWidth())
                Row (
                    Modifier
                        .fillMaxWidth(),
                ){
                    Chip("Perfect:", "${levelStatistic.perfect}", Modifier.weight(1f))
                    Chip("Great:", "${levelStatistic.great}", Modifier.weight(1f))
                }
                Row (
                    Modifier
                        .fillMaxWidth(),
                ){
                    Chip("Good:", "${levelStatistic.good}", Modifier.weight(1f))
                    Chip("Missed:", "${levelStatistic.missed}", Modifier.weight(1f))
                }
                HorizontalDivider()
                Text("Click to show less", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Click to show more", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun Chip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    SuggestionChip(
        icon = @Composable{
            Text(label)
        },
        label = @Composable{
            Text(value)
        },
        onClick = {},
        modifier = modifier.padding(2.dp)
    )
}

@Preview(showSystemUi = true)
@Composable
fun PreviewStatistics() {
    val statistics = LevelStatisticEntity(
        id = 123,
        userID = "123",
        songName = "short-test",
        difficulty = "easy",
        date = "12.12.2025",
        points = 123000f,
        perfect = 1,
        great = 2,
        good = 3,
        missed = 4,
        maxCombo = 0,
    )
    AirBeatsTheme {
        Scaffold { paddingValues ->
            Column (
                Modifier.fillMaxSize().padding(paddingValues),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Statistics(
                    statistics
                )
            }
        }
    }
}