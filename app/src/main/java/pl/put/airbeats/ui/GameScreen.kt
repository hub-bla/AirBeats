package pl.put.airbeats.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import pl.put.airbeats.LocalUser
import pl.put.airbeats.ui.components.ErrorComponent
import pl.put.airbeats.ui.components.Loading
import pl.put.airbeats.utils.bt.BluetoothManager
import pl.put.airbeats.utils.game.LevelStatistics
import pl.put.airbeats.utils.game.MyGLRenderer
import pl.put.airbeats.utils.game.MyGLSurfaceView
import pl.put.airbeats.utils.midi.MidiReader
import pl.put.airbeats.utils.midi.NoteTrack
import pl.put.airbeats.utils.room.LevelStatisticEntity
import pl.put.airbeats.utils.room.AirBeatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.net.toUri
import android.app.Activity
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog

@Composable
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
fun GameScreen(
    songName: String,
    difficulty: String,
    airBeatsViewModel: AirBeatsViewModel,
    modifier: Modifier = Modifier
) {
    var noteTracks = remember { mutableStateOf(emptyMap<String, NoteTrack>()) }
    var bpm = remember { mutableIntStateOf(0) }
    var mediaPlayer = remember { mutableStateOf(MediaPlayer()) }
    var gameState by remember { mutableStateOf(0) }
    var levelStatistics by remember { mutableStateOf<LevelStatistics?>(null) }

    val onLevelEnd = { stats: LevelStatistics ->
        levelStatistics = stats
        gameState = 2
    }

    when (gameState) {
        0 -> Menu(
            {newNoteTracks ->  noteTracks.value = newNoteTracks},
            {newBpm ->  bpm.intValue = newBpm},
            {newMediaPlayer ->  mediaPlayer.value = newMediaPlayer},
            songName,
            difficulty,
            { gameState = 1 },
            modifier,
        )

        1 -> Game(
            airBeatsViewModel,
            mediaPlayer.value,
            noteTracks.value,
            bpm.intValue,
            onLevelEnd,
            {gameState = 2},
            modifier,
        )

        2 -> LevelEnd(
            songName,
            difficulty,
            levelStatistics!!,
            {gameState = 0},
            airBeatsViewModel,
            modifier,
        )
    }
}

@Composable
fun Menu(
    changeNoteTracks: (Map<String, NoteTrack>) -> Unit,
    changeBpm: (Int) -> Unit,
    changeMediaPlayer: (MediaPlayer) -> Unit,
    songName: String,
    difficulty: String,
    startGame: () -> Unit,
    modifier: Modifier = Modifier,
    ) {
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

                if (midiLink == "") {
                    Log.d("Game", "Song document is empty")
                    isLoading.value = false
                    error.value = "Song data is not available."
                    return@addOnSuccessListener
                }

                runBlocking {
                    val midi = MidiReader()
                    val noteTracks = midi.read(midiLink, bpm)
                    Log.d("Game", "Note Tracks loaded")
                    changeNoteTracks(noteTracks)
                    changeBpm(bpm)
                }
                val mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(audioLink)
                    prepare()
                    setOnPreparedListener {
                        Log.d("audioDelay", "MediaPlayer is prepared")
                        isLoading.value = false
                    }
//                    setOnCompletionListener {
//                        it.release()
//                    }
                }
                changeMediaPlayer(mediaPlayer)
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
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
fun Game(
        airBeatsViewModel: AirBeatsViewModel,
        mediaPlayer: MediaPlayer,
        noteTracks: Map<String, NoteTrack>,
        bpm: Int,
        onLevelEnd: (LevelStatistics) -> Unit,
        changeState: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
    val glViewRef = remember { mutableStateOf<MyGLSurfaceView?>(null) }
    val rendererRef = remember { mutableStateOf<MyGLRenderer?>(null) }
    val isConnected = remember { mutableStateOf(false) }
    val bluetoothManager = remember { mutableStateOf(BluetoothManager()) }

    val isSavingEnergy by airBeatsViewModel.isSavingEnergy.collectAsState()

//    BackHandler {
//        onLevelEnd(LevelStatistics())
//        bluetoothManager.value.disconnect()
//        mediaPlayer.stop()
//    }

    DisposableEffect(Unit) {
        onDispose {
            //onLevelEnd(LevelStatistics())
            changeState()
            bluetoothManager.value.disconnect()
            mediaPlayer.stop()
        }
    }
    //array [stick id][float pos][event]
    LaunchedEffect(isConnected) {
        if (isConnected.value) {
            Log.d("INIT BLE L:ISTening", "")
            bluetoothManager.value.startReceivingLoop(glViewRef.value!!) { data ->
                rendererRef.value?.columnEvent = data[2].toInt()
                rendererRef.value?.hasEventOccured = data[2].toInt() != 9
                if(data[0] == "r"){
                    rendererRef.value?.rightStickPos = data[1].toFloat() / 180 - 1
                }
                else if (data[0] == "l"){
                    rendererRef.value?.leftStickPos = data[1].toFloat() / 180 - 1
                }


                Log.d("DATARECV", data.toString())
            }
        } else {
            isConnected.value = bluetoothManager.value.connectToDevice("airdrums")
            Log.d("INIT BLE L:ISTening", "")
            bluetoothManager.value.startReceivingLoop(glViewRef.value!!) { data ->
                rendererRef.value?.columnEvent = data[2].toInt()
                rendererRef.value?.hasEventOccured = data[2].toInt() != 9
                if(data[0] == "r"){
                    rendererRef.value?.rightStickPos = data[1].toFloat() / 180 - 1
                }
                else if (data[0] == "l"){
                    rendererRef.value?.leftStickPos = data[1].toFloat() / 180 - 1
                }
                Log.d("DATARECV", data.toString())
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory =  { context ->
            val glView = MyGLSurfaceView(context, noteTracks, bpm,
                { mediaPlayer.start() },
                isSavingEnergy,
                { stats ->
                    onLevelEnd(stats)
                    bluetoothManager.value.disconnect()
                }
            )
            glViewRef.value = glView
            rendererRef.value = glView.renderer
            glView
        }
    )
}

@Composable
fun LevelEnd(
    songName: String,
    difficulty: String,
    stats: LevelStatistics,
    startGame: () -> Unit,
    levelStatisticviewModel: AirBeatsViewModel,
    modifier: Modifier = Modifier,
    ) {
    val hasSavedStatistics = remember { mutableStateOf(false) }
    val message = remember { mutableStateOf("") }
    val userID = LocalUser.current.value

    val onSave = {
        if(!hasSavedStatistics.value){
            val formater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val newLevelStatistcRow = LevelStatisticEntity(
                userID = userID,
                songName = songName,
                difficulty = difficulty,
                date = LocalDateTime.now().format(formater),
                points = stats.points,
                perfect = stats.perfect,
                great = stats.great,
                good = stats.good,
                missed = stats.missed,
                maxCombo = stats.maxCombo
            )
            levelStatisticviewModel.insert(newLevelStatistcRow)
            hasSavedStatistics.value = true
            message.value = "Your statistics have been saved"
        } else {
            message.value = "Your statistics cannot be saved more than one time"
        }
    }
    val levelStatistics by levelStatisticviewModel.selectUser(userID).collectAsState(emptyList())

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FacebookShareButton(stats.points)
        Text("Level ended\nPoints:${stats.points}")

        Text("Level Statistics")
        Text("Points: ${stats.points}")
        Text("Max Combo: ${stats.maxCombo}")
        Text("Perfect: ${stats.perfect}")
        Text("Great: ${stats.great}")
        Text("Good: ${stats.good}")
        Text("Missed: ${stats.missed}")

        Button(onClick = onSave) {
            Text("save statistics")
        }

        if(message.value != ""){
            Text(message.value)
        }

        Button(onClick = startGame) {
            Text("play again")
        }

        Text("All Player Statistics")

        LazyColumn {
            items(levelStatistics) { levelStatistic ->
                Row {
                    Text("Date: ${levelStatistic.date} points: ${levelStatistic.points} missed: ${levelStatistic.missed}")
                }
            }
        }
    }
}

@Composable
fun FacebookShareButton(
    points: Float,
    urlToShare: String = "https://github.com/hub-bla/AirBeats"
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val quote = "Got $points points in AirBeats!"
    var shareDialog by remember { mutableStateOf<ShareDialog?>(null) }

    LaunchedEffect(Unit) {
        if (activity != null) {
            val canShow = try {
                ShareDialog.canShow(ShareLinkContent::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
            if (canShow) {
                shareDialog = ShareDialog(activity)
            } else {
                Log.d("FBShareButton", "Cannot show ShareDialog")
            }
        } else {
            Log.d("FBShareButton", "Context is not an Activity")
        }
    }

    Button(
        onClick = {
            if (shareDialog == null) {
                Log.d("FBShareButton", "ShareDialog not initialized")
                return@Button
            }
            val content = ShareLinkContent.Builder()
                .setContentUrl(urlToShare.toUri())
                .setQuote(quote)
                .build()

            shareDialog?.show(content)
        },
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text("Share on Facebook")
    }
}