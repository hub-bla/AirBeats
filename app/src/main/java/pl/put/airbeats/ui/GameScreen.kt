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
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import pl.put.airbeats.utils.LottieLoading

@Composable
@androidx.annotation.RequiresPermission(android.Manifest.permission.BLUETOOTH_CONNECT)
fun GameScreen(
    songName: String,
    difficulty: String,
    airBeatsViewModel: AirBeatsViewModel,
    modifier: Modifier = Modifier,
    navController: NavController,
    onBackToSongs: (() -> Unit)? = null
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showCalibrationModal) {
            CalibrationModal(
                onAccept = onCalibrationAccepted,
                onCancel = onCalibrationCancelled
            )
        }

        BackHandler(
            enabled = gameState == 0 || gameState == 2
        ) {
            val previousEntry = navController.previousBackStackEntry
            Log.d("BackHandler", "Previous entry: $previousEntry")
            Log.d("BackHandler", "Setting difficulty: $difficulty")

            previousEntry?.savedStateHandle?.set("difficulty", difficulty)
            navController.popBackStack()
        }

        when (gameState) {
            0 -> Menu(
                {newNoteTracks ->  noteTracks.value = newNoteTracks},
                {newBpm ->  bpm.intValue = newBpm},
                {newMediaPlayer ->  mediaPlayer.value = newMediaPlayer},
                songName,
                difficulty,
                startCalibration,
                Modifier.fillMaxSize(),
            )

            1 -> Game(
                airBeatsViewModel,
                mediaPlayer.value,
                noteTracks.value,
                bpm.intValue,
                onLevelEnd,
                {gameState = 2},
                Modifier.fillMaxSize(),
            )

            2 -> LevelEnd(
                songName,
                difficulty,
                levelStatistics!!,
                {gameState = 0},
                airBeatsViewModel,
                Modifier.fillMaxSize(),
            )
        }
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
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Stick Calibration",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "You need to calibrate the sensors before starting the game.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "Instructions:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "• Place the sticks in a resting position",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Make sure they are stable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Do not move the sticks during calibration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("Cancel")
                    }

                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calibrate")
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

                val noteTracks = withContext(Dispatchers.IO) {
                    val midi = MidiReader()
                    midi.read(midiLink, bpm)
                }

                Log.d("Game", "Note Tracks loaded")
                changeNoteTracks(noteTracks)
                changeBpm(bpm)

                val mediaPlayer = withContext(Dispatchers.IO) {
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setDataSource(audioLink)
                        prepareAsync()
                        setOnPreparedListener {
                            Log.d("audioDelay", "MediaPlayer is prepared")
                            isLoading = false
                        }
                    }
                }

                changeMediaPlayer(mediaPlayer)

            } catch (e: Exception) {
                Log.d("Firestore failure", "couldn't fetch data: ${e.message}")
                error = "Couldn't fetch song data: ${e.message}"
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        if (isLoading) {
            LottieLoading(message = "Gathering data: ${songName}...")
            return
        }
        else{
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = songName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        ErrorComponent(error)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = startGame,
                        enabled = error.isEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PLAY",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
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
    val context = LocalContext.current
    val activity = context.findActivity()
    val glViewRef = remember { mutableStateOf<MyGLSurfaceView?>(null) }
    val rendererRef = remember { mutableStateOf<MyGLRenderer?>(null) }
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(true) }
    var hasReceivedFirstMessage by remember { mutableStateOf(false) }
    var isGamePaused by remember { mutableStateOf(false) }
    var gameStarted by remember { mutableStateOf(false) }
    var mediaPlayerPosition by remember { mutableIntStateOf(0) }
    var wasMediaPlayerPlaying by remember { mutableStateOf(false) }
    val bluetoothManager = remember { mutableStateOf(BluetoothManager()) }
    var preventAutoResume by remember { mutableStateOf(false) }

    val isSavingEnergy by airBeatsViewModel.isSavingEnergy.collectAsState()
    val scope = rememberCoroutineScope()

    val startMediaPlayer: ()->Unit = {
        try {
            if (!preventAutoResume) {
                if (mediaPlayerPosition > 0) {
                    Log.d("Game", "Resuming MediaPlayer from position: $mediaPlayerPosition")
                    mediaPlayer.seekTo(mediaPlayerPosition)
                }
                mediaPlayer.start()
                gameStarted = true
                Log.d("Game", "MediaPlayer started")
            }
        } catch (e: Exception) {
            Log.e("Game", "Error starting MediaPlayer: ${e.message}")
        }
    }

    val pauseMediaPlayer = {
        try {
            preventAutoResume = true
            if (mediaPlayer.isPlaying) {
                wasMediaPlayerPlaying = true
                mediaPlayerPosition = mediaPlayer.currentPosition
                mediaPlayer.pause()
                Log.d("Game", "MediaPlayer paused manually at position: $mediaPlayerPosition")
            }
            else{

            }
        } catch (e: Exception) {
            Log.e("Game", "Error pausing MediaPlayer: ${e.message}")
        }
    }

    val resumeMediaPlayer = {
        try {
            preventAutoResume = false
            if (wasMediaPlayerPlaying) {
                Log.d("Game", "Resuming MediaPlayer from position: $mediaPlayerPosition")
                mediaPlayer.seekTo(mediaPlayerPosition)
                mediaPlayer.start()
                Log.d("Game", "MediaPlayer resumed")
            }
            else{

            }
        } catch (e: Exception) {
            Log.e("Game", "Error resuming MediaPlayer: ${e.message}")
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                Log.d("Game", "Activity paused - pausing game")
                if (gameStarted && !isGamePaused) {
                    isGamePaused = true
                    try {
                        if (mediaPlayer.isPlaying) {
                            wasMediaPlayerPlaying = true
                            pauseMediaPlayer()
                            Log.d("Game", "MediaPlayer paused at position: $mediaPlayerPosition")
                        } else {
                            wasMediaPlayerPlaying = false
                        }
                        glViewRef.value?.onPause()
                    } catch (e: Exception) {
                        Log.e("Game", "Error pausing game: ${e.message}")
                    }
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                Log.d("Game", "Activity resumed")
                if (isGamePaused) {
                    Log.d("Game", "Game is paused - waiting for manual resume")
                    glViewRef.value?.onResume()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(Unit) {
        activity?.let {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            it.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            changeState()
            bluetoothManager.value.disconnect()
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (e: Exception) {
                Log.e("Game", "Error stopping media player: ${e.message}")
            }
        }
    }

    DisposableEffect(mediaPlayer) {
        val onCompletionListener = MediaPlayer.OnCompletionListener {
            Log.d("Game", "MediaPlayer completed")
        }

        val onErrorListener = MediaPlayer.OnErrorListener { _, what, extra ->
            Log.e("Game", "MediaPlayer error: what=$what, extra=$extra")
            true
        }

        mediaPlayer.setOnCompletionListener(onCompletionListener)
        mediaPlayer.setOnErrorListener(onErrorListener)

        onDispose {
            try {
                mediaPlayer.setOnCompletionListener(null)
                mediaPlayer.setOnErrorListener(null)
            } catch (e: Exception) {
                Log.e("Game", "Error clearing MediaPlayer listeners: ${e.message}")
            }
        }
    }

    val connectToDevice = suspend {
        isConnecting = true
        hasReceivedFirstMessage = false
        try {
            val connected = withContext(Dispatchers.IO) {
                bluetoothManager.value.connectToDevice("airdrums")
            }
            isConnected = connected
            Log.d("BLE_CONNECTION", "Connection result: $connected")
        } catch (e: Exception) {
            Log.e("BLE_CONNECTION", "Connection failed: ${e.message}")
            isConnected = false
        } finally {
            isConnecting = false
        }
    }



    LaunchedEffect(Unit) {
        val glView = MyGLSurfaceView(
            context,
            noteTracks,
            bpm,
            startMediaPlayer,
            isSavingEnergy,
            { stats ->
                onLevelEnd(stats)
                bluetoothManager.value.disconnect()
            }
        )
        glView.setZOrderOnTop(true)
        glView.holder.setFormat(PixelFormat.TRANSLUCENT)
        glViewRef.value = glView
        rendererRef.value = glView.renderer
        Log.d("Game", "GLSurfaceView created early")

        connectToDevice()
    }

    LaunchedEffect(isConnected, glViewRef.value) {
        if (isConnected && glViewRef.value != null) {
            Log.d("BLE_LISTENING", "Starting receiving loop")
            try {
                bluetoothManager.value.startReceivingLoop(glViewRef.value!!) { data ->
                    if (!hasReceivedFirstMessage) {
                        hasReceivedFirstMessage = true
                        Log.d("BLE_LISTENING", "First message received")
                    }

                    if (!isGamePaused) {
                        rendererRef.value?.columnEvent = data[2].toInt()
                        rendererRef.value?.hasEventOccured = data[2].toInt() != 9
                        if(data[0] == "r"){
                            rendererRef.value?.rightStickPos = data[1].toFloat() / 180 - 1
                        }
                        else if (data[0] == "l"){
                            rendererRef.value?.leftStickPos = data[1].toFloat() / 180 - 1
                        }
                    }
                    Log.d("DATARECV", data.toString())
                }
            } catch (e: Exception) {
                Log.e("BLE_LISTENING", "Error in receiving loop: ${e.message}")
                isConnected = false
                hasReceivedFirstMessage = false
            }
        }
    }

    BackHandler(enabled = !isGamePaused && isConnected && hasReceivedFirstMessage) {
        Log.d("Game", "Back button pressed - pausing game")
        isGamePaused = true
        pauseMediaPlayer()
        glViewRef.value?.onPause()
    }

    if (!isConnected || !hasReceivedFirstMessage) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isConnecting -> {
                    LottieLoading(
                        message = "Connecting to Bluetooth device...",
                        modifier = Modifier.fillMaxSize())

                }
                !isConnected && !isConnecting -> {

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = "Failed to connect to Bluetooth device",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    bluetoothManager.value.disconnect()
                                    scope.launch {
                                        connectToDevice()
                                    }
                                },
                                enabled = !isConnecting,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Retry Connection",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedButton(
                                onClick = {
                                    bluetoothManager.value.disconnect()
                                    isConnected = true
                                    hasReceivedFirstMessage = true
                                },
                                enabled = !isConnecting,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = "Anyway...",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
                isConnected && !hasReceivedFirstMessage -> {
                    LottieLoading(message = "Calibrating sensors...", modifier = Modifier.fillMaxSize())
                }
            }
        }
        return
    }

    if (isGamePaused) {

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.9f)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 8.dp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Game Paused",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            Log.d("Game", "Resume button pressed")
                            isGamePaused = false
                            glViewRef.value?.onResume()
                            resumeMediaPlayer()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Resume Game",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = {
                            Log.d("Game", "Exit button pressed")
                            try {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.stop()
                                }
                            } catch (e: Exception) {
                                Log.e("Game", "Error stopping MediaPlayer on exit: ${e.message}")
                            }
                            bluetoothManager.value.disconnect()
                            onLevelEnd(LevelStatistics())
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Exit Game",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        return
    }

    if (glViewRef.value != null) {
        AndroidView(
            modifier = modifier.fillMaxSize(),
            factory = { glViewRef.value!! }
        )
    }
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

    val onSave: () -> Unit = {
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
    }

    val levelStatistics by levelStatisticviewModel.selectUser(userID).collectAsState(emptyList())

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FacebookShareButton(stats.points)
        Text("Level ended\nPoints:${stats.points}")
        //no points for unfinished level
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