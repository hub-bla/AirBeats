package pl.put.airbeats.utils.game

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import pl.put.airbeats.utils.midi.NoteTrack
import java.util.Timer
import kotlin.concurrent.schedule
import kotlin.math.abs

// Game related constants
const val LINE_HEIGHT = -0.7f // top = 1f, bottom = -1f
const val STICK_HEIGHT = 0f // top = 1f, bottom = -1f
const val TILE_ON_SCREEN_TIME = 1500f // in milliseconds
const val TILE_SPEED = 2f / TILE_ON_SCREEN_TIME
const val START_DELAY = 2000f // in milliseconds

// Scoring related constants
const val ERROR_MARGIN = 1.1f // relative to tile center (gives additional invisible error margin if >1f )

//const val PERFECT_MARGIN = 0.1f // relative to tile center
//const val GREAT_MARGIN = 0.5f // relative to tile center
//const val GOOD_MARGIN = 1.1f // relative to tile center (gives additional invisible error margin if >1f )
//const val PERFECT_POINTS = 100f
//const val GREAT_POINTS = 50f
//const val GOOD_POINTS = 10f
//const val PERFECT_COMBO_MULT = 1.1f


class MyGLRenderer : GLSurfaceView.Renderer {
    // Render variables
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var tiles = mutableListOf<MutableList<Tile>>()

    private lateinit var line: Tile
    private lateinit var leftStick: Dot
    private lateinit var rightStick: Dot

    private var lastTime: Long = 0

    private lateinit var shaderProgram: ShaderProgram


    // Level variables
    private var noteTracks: Map<String, NoteTrack>
//    private var bpm: Int
    private val playAudio: () -> Unit
    private val onLevelEnd: (LevelStatistics) -> Unit

    // Level statistics variables
    private var stats = LevelStatistics()
    private var initialized = false

//    private var points = 0f
//    private var maxCombo = 0
//    private var combo = 0
//    private var comboMultiplier = 1f
//    private var perfect = 0
//    private var great = 0
//    private var good = 0
//    private var missed = 0

    // Event variables
    @Volatile
    var hasEventOccured = false
    @Volatile
    var columnEvent = 0
    @Volatile
    var leftStickPos = -0.5f // normalized to range from -1 (left) to 1 (right)
    @Volatile
    var rightStickPos = 0.5f // normalized to range from -1 (left) to 1 (right)

    // Other variables
    val hitTileColor = floatArrayOf(0.118f, 0.863f, 0.133f, 1.0f)
    val missedTileColor = floatArrayOf(0.818f, 0.163f, 0.133f, 1.0f)
    val leftStickColor = floatArrayOf(0.133f, 0.818f, 0.133f, 1.0f)
    val rightStickColor = floatArrayOf(0.818f, 0.818f, 0.133f, 1.0f)

    constructor(noteTracks: Map<String, NoteTrack>, bpm: Int, playAudio: () -> Unit, onLevelEnd: (LevelStatistics) -> Unit,) {
        this.noteTracks = noteTracks
//        this.bpm = bpm
        this.playAudio = playAudio
        this.onLevelEnd = onLevelEnd
    }

    private var gamePaused = false
    private var pauseTime = 0L

    fun pauseGame() {
        gamePaused = true
        pauseTime = System.currentTimeMillis()
    }

    fun resumeGame() {
        if (gamePaused) {
            val pauseDuration = System.currentTimeMillis() - pauseTime

            // Adjust lastTime to account for the pause duration
            // This prevents a large time jump in the next frame
            lastTime += pauseDuration

            // Reset pause state
            gamePaused = false
            pauseTime = 0L

            Log.d("Game", "Game resumed after ${pauseDuration}ms pause")
        }
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {


        // Creates shader program
        this.shaderProgram = ShaderProgram()

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Set number of columns
        val numberOfColumns = 4

        // Set colors of tiles in columns
        val tileColors = mutableListOf<FloatArray>()
        for(i in 0..<numberOfColumns step 2) {
            tileColors.add(floatArrayOf(0.118f, 0.663f, 0.933f, 1.0f))
            tileColors.add(floatArrayOf(0.018f, 0.563f, 0.833f, 1.0f))
        }

        // Calculate tile width
        val tileWidth = 2f / numberOfColumns
        val tileWidthOffset = tileWidth / 2f

        // Calculate starting x coordinate of first column
        val startPositionXOffset = -1f + tileWidthOffset

        // Calculate starting y coordinate
        val startPositionYOffset = 1f + TILE_SPEED * START_DELAY

        // Calculate starting positions for all columns
        val startPositions = mutableListOf<FloatArray>()
        for (i in 0..<numberOfColumns) {
            val startPosition = floatArrayOf(startPositionXOffset + i*tileWidth, startPositionYOffset, 0f)
            startPositions.add(startPosition)
        }

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f, // Camera position
            0f, 0f, 0f, // Camera looks at
            0f, 1.0f, 0.0f // Camera up
        )

        Matrix.frustumM(projectionMatrix, 0,
            -1f, 1f,    // left right
            -1f, 1f,    // bottom top
            3f, 7f      // near far
        )

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Create and setup all Tile objects on level
        val position = FloatArray(16)
        var currentColumn = 0
        if(!initialized) {
            initialized = true
            for ((_, noteTrack) in noteTracks) {
                if (currentColumn >= numberOfColumns) {
                    break
                }
                val startPosition = startPositions[currentColumn]
                val (x, y, z) = startPosition
                val color = tileColors[currentColumn]
                val tilesInColumn = mutableListOf<Tile>()
                for (noteDurationTimestamp in noteTrack.noteOnsTimesInMs) {
                    // Calculate duration of note
                    val (noteStartTime, noteEndTime) = noteDurationTimestamp
                    val noteDuration = noteEndTime - noteStartTime

                    // Calculate height of tile based on duration of note
                    val noteHeight = (noteDuration.toFloat() * TILE_SPEED) * 0.95f
                    val startPositionYOffset = noteHeight / 2f
//                val startPositionYOffset =  0

                    // Calculate y coordinate of tile based on note timestamp
                    val distance = noteStartTime.toFloat() * TILE_SPEED
                    val yDistance = y + distance + startPositionYOffset

                    // Calculate tile position
                    Matrix.setIdentityM(position, 0)
                    Matrix.translateM(position, 0, x, yDistance, z)

                    // Create Tile Object
                    val tile = Tile(shaderProgram, tileWidth, noteHeight, position, vpMatrix, color)
                    tilesInColumn.add(tile)
                    stats.addTile()
                }
                tiles.add(tilesInColumn)
                currentColumn++
            }
            for (i in tiles.size..<numberOfColumns) {
                val tilesInColumn = mutableListOf<Tile>()
                tiles.add(tilesInColumn)
            }

        }
        // Calculate line position
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, 0f, LINE_HEIGHT, -0.1f)

        line = Tile(
            shaderProgram,
            2.5f,
            0.05f,
            position,
            vpMatrix,
            floatArrayOf(0.818f, 0.163f, 0.133f, 1f))


        // Calculate left stick position
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, leftStickPos, STICK_HEIGHT, -0.1f)

        leftStick = Dot(
            shaderProgram,
            0.1f,
            0.045f,
            position,
            vpMatrix,
            leftStickColor)


        // Calculate right stick position
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, rightStickPos, STICK_HEIGHT, -0.1f)

        rightStick = Dot(
            shaderProgram,
            0.1f,
            0.045f,
            position,
            vpMatrix,
            rightStickColor)

        val audioDelay = (START_DELAY + TILE_ON_SCREEN_TIME * ((2 + LINE_HEIGHT)/ 2)).toLong()
        Log.d("audioDelay", "$audioDelay")

        lastTime = SystemClock.uptimeMillis()
        Timer("SettingUp", false).schedule(audioDelay) {
            playAudio()
        }
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        if (gamePaused) {
            // Don't update game logic when paused, but still render the last frame
            return
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val currentTime = SystemClock.uptimeMillis()
        val time = currentTime - lastTime
        lastTime = currentTime

        val distance = -TILE_SPEED * time.toInt()

        if(hasEventOccured) {
            Log.d("Game event", "Event at column $columnEvent")
            hasEventOccured = false
            if(tiles[columnEvent].isNotEmpty()) {
                var relativeOffset = 2f
                tiles[columnEvent].firstOrNull{ tile ->
                    val (_, y, _, _) = tile.getPosition()
                    relativeOffset = (abs(LINE_HEIGHT - y)) / (tile.tileHeight / 2f)
                    (relativeOffset < ERROR_MARGIN)
                }?.let {
                    Log.d("Game event", "Tile hit with relative offset $relativeOffset")
                    stats.score(relativeOffset)
                    it.changeColor(hitTileColor)
                    Log.d("Game event", "Current points ${stats.points}")
                }
            }
        }

        // TODO tile coloring after miss
//        tiles.forEach { tilesInColumn ->
//            tilesInColumn.firstOrNull { tile ->
//                val (_, y, _, _) = tile.getPosition()
//                (y + tile.tileHeight / 2f) < LINE_HEIGHT
//            }?.changeColor(missedTileColor)
//        }

        for(tile in tiles.flatten()){
            tile.move(0f, distance, 0f)
            tile.draw()
        }
        line.draw()
        leftStick.draw(leftStickPos, STICK_HEIGHT, -0.1f)
        rightStick.draw(rightStickPos, STICK_HEIGHT, -0.1f)
        tiles.forEach { tilesInColumn ->
            tilesInColumn.removeIf { tile ->
                val (_, y, _, _) = tile.getPosition()
                (y + tile.tileHeight / 2f) < -1.0
            }
        }

        if( tiles.flatten().isEmpty() ) {
            onLevelEnd(stats)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}