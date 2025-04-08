package pl.put.airbeats.utils.game

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import android.util.Log
import pl.put.airbeats.utils.midi.NoteTrack

class MyGLRenderer : GLSurfaceView.Renderer {
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var tileSpeed: Float = 1f

    private var tiles = mutableListOf<Tile>()

    private var lastTime: Long = 0

    private var noteTracks: Map<String, NoteTrack>
    private var bpm: Int = 180

    private lateinit var shaderProgram: ShaderProgram


    constructor(noteTracks: Map<String, NoteTrack>, bpm: Int) {
        this.noteTracks = noteTracks
        this.bpm = bpm
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Creates shader program
        this.shaderProgram = ShaderProgram()

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Set speed of tile animation
        val tileOnScreenTime = 1500f // in milliseconds
        tileSpeed = 2f/tileOnScreenTime

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
        val startDelayTime = 2000f // in milliseconds
        val startPositionYOffset = 1f + tileSpeed * startDelayTime

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

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
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
        for((_, noteTrack) in noteTracks){
            if(currentColumn >= numberOfColumns){
                break
            }
            val startPosition = startPositions[currentColumn]
            val (x, y, z) = startPosition
            val color = tileColors[currentColumn]
            for (noteDurationTimestamp in noteTrack.noteOnsTimesInMs){
                // Calculate duration of note
                val (noteStartTime, noteEndTime) = noteDurationTimestamp
                val noteDuration = noteEndTime - noteStartTime

                // Calculate height of tile based on duration of note
                val noteHeight = (noteDuration.toFloat() * tileSpeed) * 0.95f
                val startPositionYOffset =  noteHeight/2f

                // Calculate y coordinate of tile based on note timestamp
                val distance = noteStartTime.toFloat() * tileSpeed
                val yDistance = y + distance + startPositionYOffset

                // Calculate tile position
                Matrix.setIdentityM(position, 0)
                Matrix.translateM(position, 0, x, yDistance, z)

                // Create Tile Object
                val tile = Tile(shaderProgram, tileWidth, noteHeight, position, vpMatrix, color)
                tiles.add(tile)
            }
            currentColumn++
        }

        lastTime = SystemClock.uptimeMillis()
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val currentTime = SystemClock.uptimeMillis()
        val time = currentTime - lastTime
        lastTime = currentTime

        val distance = -tileSpeed * time.toInt()

        for(tile in tiles){
            tile.move(0f, distance, 0f)
            val (x,y,z,w) = tile.getPosition()
            Log.d("Tile", "x:$x; y:$y; z:$z; w:$w\n")
            tile.draw()
        }
        tiles.removeIf { tile ->
            val (x,y,z,w) = tile.getPosition()
            y + tile.tileHeight/2.0 < -1.0
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
}