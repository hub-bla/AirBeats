package pl.put.airbeats.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.put.airbeats.LocalUser
import pl.put.airbeats.ui.components.Statistics
import pl.put.airbeats.utils.room.AirBeatsViewModel

@Composable
fun StatisticsScreen(
    levelStatisticviewModel: AirBeatsViewModel,
    modifier: Modifier = Modifier
) {
    val userID = LocalUser.current.value
    val levelStatistics by levelStatisticviewModel.selectUser(userID).collectAsState(emptyList())

    Scaffold(modifier = modifier) { paddingValues ->
        Column(Modifier.padding(paddingValues).fillMaxSize()) {
            Text("Statistics:", modifier = Modifier.align(Alignment.CenterHorizontally), fontSize = 24.sp)

            LazyColumn (
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(levelStatistics) { levelStatistic ->

                    Card(Modifier.padding(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Statistics(
                                levelStatistic
                            )
                            Row (Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                Button(
                                    onClick = { levelStatisticviewModel.delete(levelStatistic) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Delete statistic from local database")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}