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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
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
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.share.Sharer
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
    var showCalibrationModal by remember { mutableStateOf(false) }

    val onLevelEnd = { stats: LevelStatistics ->
        levelStatistics = stats
        gameState = 2
    }

    val startCalibration = {
        showCalibrationModal = true
    }

    val onCalibrationAccepted = {
        showCalibrationModal = false
        gameState = 1
    }

    val onCalibrationCancelled = {
        showCalibrationModal = false
    }

    // Modal kalibracji
    if (showCalibrationModal) {
        CalibrationModal(
            onAccept = onCalibrationAccepted,
            onCancel = onCalibrationCancelled
        )
    }

    when (gameState) {
        0 -> Menu(
            {newNoteTracks ->  noteTracks.value = newNoteTracks},
            {newBpm ->  bpm.intValue = newBpm},
            {newMediaPlayer ->  mediaPlayer.value = newMediaPlayer},
            songName,
            difficulty,
            startCalibration,
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
fun CalibrationModal(
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Kalibracja pałek",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Przed rozpoczęciem gry należy skalibrować pałki drumstick'ów.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Instrukcje:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• Ustaw pałki w pozycji spoczynkowej",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Upewnij się, że pałki są stabilne",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Nie ruszaj pałkami podczas kalibracji",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Anuluj")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Rozpocznij kalibrację")
                    }
                }
            }
        }
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
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(difficulty, songName) {
        Log.d("Game", "LaunchedEffect")
        isLoading = true
        error = ""

        scope.launch {
            try {
                val db = Firebase.firestore

                // Pobranie danych z Firestore w korutynie
                val document = withContext(Dispatchers.IO) {
                    db.collection("${difficulty}_songs").document(songName).get().await()
                }

                Log.d("Firestore success", "Song document loaded")
                val midiLink = document.get("midi").toString()
                val audioLink = document.get("audio").toString()
                val bpm = document.get("bpm").toString().toInt()

                if (midiLink.isEmpty()) {
                    Log.d("Game", "Song document is empty")
                    error = "Song data is not available."
                    isLoading = false
                    return@launch
                }

                // Przetwarzanie MIDI w tle
                val noteTracks = withContext(Dispatchers.IO) {
                    val midi = MidiReader()
                    midi.read(midiLink, bpm)
                }

                Log.d("Game", "Note Tracks loaded")
                changeNoteTracks(noteTracks)
                changeBpm(bpm)

                // Przygotowanie MediaPlayer w tle
                val mediaPlayer = withContext(Dispatchers.IO) {
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(audioLink)
                        prepare()
                    }
                }

                changeMediaPlayer(mediaPlayer)
                Log.d("audioDelay", "MediaPlayer is prepared")
                isLoading = false

            } catch (e: Exception) {
                Log.d("Firestore failure", "couldn't fetch data: ${e.message}")
                error = "Couldn't fetch song data: ${e.message}"
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(songName)

        if (isLoading) {
            Loading()
            return
        }

        if (error.isNotEmpty()) {
            ErrorComponent(error)
        }

        Button(onClick = startGame, enabled = error.isEmpty()) {
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
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    val bluetoothManager = remember { mutableStateOf(BluetoothManager()) }
    val scope = rememberCoroutineScope()

    val isSavingEnergy by airBeatsViewModel.isSavingEnergy.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            changeState()
            scope.launch {
                withContext(Dispatchers.IO) {
                    bluetoothManager.value.disconnect()
                    try {
                        mediaPlayer.stop()
                    } catch (e: Exception) {
                        Log.e("Game", "Error stopping media player: ${e.message}")
                    }
                }
            }
        }
    }

    // Połączenie Bluetooth w korutynie
    LaunchedEffect(Unit) {
        if (!isConnected && !isConnecting) {
            isConnecting = true
            scope.launch {
                try {
                    val connected = withContext(Dispatchers.IO) {
                        bluetoothManager.value.connectToDevice("airdrums")
                    }
                    isConnected = connected

                    if (connected) {
                        Log.d("INIT BLE LISTENING", "Connected successfully")
                    } else {
                        Log.e("INIT BLE LISTENING", "Failed to connect")
                    }
                } catch (e: Exception) {
                    Log.e("INIT BLE LISTENING", "Connection error: ${e.message}")
                    isConnected = false
                } finally {
                    isConnecting = false
                }
            }
        }
    }

    // Uruchomienie receiving loop po połączeniu - bez dodatkowych korutyn
    LaunchedEffect(isConnected, glViewRef.value) {
        if (isConnected && glViewRef.value != null) {
            try {
                Log.d("INIT BLE LISTENING", "Starting receiving loop")
                // Uruchamiamy bezpośrednio bez dodatkowej korutyny dla maksymalnej wydajności
                bluetoothManager.value.startReceivingLoop(glViewRef.value!!) { data ->
                    // Bezpośrednie przypisanie bez korutyn - najszybsze
                    // Usuwamy logi z krytycznej ścieżki dla lepszej wydajności
                    rendererRef.value?.apply {
                        val eventValue = data[2].toInt()
                        columnEvent = eventValue
                        hasEventOccured = eventValue != 9

                        // Optymalizacja - unikamy wielokrotnego parsowania
                        val position = data[1].toFloat() / 180f - 1f
                        when (data[0]) {
                            "r" -> rightStickPos = position
                            "l" -> leftStickPos = position
                        }
                    }
                    // Przeniesiono log poza krytyczną ścieżkę

                    Log.d("DATARECV", data.toString())

                }
            } catch (e: Exception) {
                Log.e("INIT BLE LISTENING", "Error in receiving loop: ${e.message}")
            }
        }
    }

    if (isConnecting) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Loading()
            Text("Connecting to Bluetooth device...")
        }
        return
    }

    if (!isConnected) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Failed to connect to Bluetooth device")
            Button(
                onClick = {
                    scope.launch {
                        isConnecting = true
                        try {
                            val connected = withContext(Dispatchers.IO) {
                                bluetoothManager.value.connectToDevice("airdrums")
                            }
                            isConnected = connected
                            if (connected) {
                                Log.d("Bluetooth", "Reconnected successfully")
                            }
                        } catch (e: Exception) {
                            Log.e("Bluetooth", "Retry connection error: ${e.message}")
                        } finally {
                            isConnecting = false
                        }
                    }
                }
            ) {
                Text("Retry Connection")
            }
        }
        return
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val glView = MyGLSurfaceView(
                context,
                noteTracks,
                bpm,
                { mediaPlayer.start() },
                isSavingEnergy,
                { stats ->
                    onLevelEnd(stats)
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            bluetoothManager.value.disconnect()
                        }
                    }
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
    var hasSavedStatistics by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    val userID = LocalUser.current.value
    val scope = rememberCoroutineScope()

    val onSave: ()->Unit = {
        if (!hasSavedStatistics && !isSaving) {
            isSaving = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        val newLevelStatisticRow = LevelStatisticEntity(
                            userID = userID,
                            songName = songName,
                            difficulty = difficulty,
                            date = LocalDateTime.now().format(formatter),
                            points = stats.points,
                            perfect = stats.perfect,
                            great = stats.great,
                            good = stats.good,
                            missed = stats.missed,
                            maxCombo = stats.maxCombo
                        )
                        levelStatisticviewModel.insert(newLevelStatisticRow)
                    }
                    hasSavedStatistics = true
                    message = "Your statistics have been saved"
                } catch (e: Exception) {
                    Log.e("LevelEnd", "Error saving statistics: ${e.message}")
                    message = "Error saving statistics: ${e.message}"
                } finally {
                    isSaving = false
                }
            }
        } else if (hasSavedStatistics) {
            message = "Your statistics cannot be saved more than one time"
        }
        else{

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

        Button(
            onClick = onSave,
            enabled = !isSaving
        ) {
            if (isSaving) {
                Text("Saving...")
            } else {
                Text("save statistics")
            }
        }

        if (message.isNotEmpty()) {
            Text(message)
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
    var isSharing by remember { mutableStateOf(false) }

    val quote = "Got $points points in AirBeats!"

    // Reużywamy dialogu
    val shareDialog = remember {
        activity?.let { ShareDialog(it) }
    }

    Button(
        onClick = {
            if (activity == null) {
                Log.d("FBShareButton", "Context is not an Activity")
                return@Button
            }

            if (!ShareDialog.canShow(ShareLinkContent::class.java)) {
                Log.d("FBShareButton", "Cannot show ShareDialog")
                return@Button
            }

            isSharing = true
            try {
                val content = ShareLinkContent.Builder()
                    .setContentUrl(urlToShare.toUri())
                    .setQuote(quote)
                    .build()

                shareDialog?.show(content)
            } catch (e: Exception) {
                Log.e("FBShareButton", "Error sharing: ${e.message}")
            } finally {
                isSharing = false
            }
        },
        enabled = !isSharing,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        if (isSharing) {
            Text("Sharing...")
        } else {
            Text("Share on Facebook")
        }
    }
}