package pl.put.airbeats.utils.game

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import pl.put.airbeats.utils.midi.NoteTrack

class MyGLRenderer : GLSurfaceView.Renderer {
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val matrixVP = FloatArray(16)

    private lateinit var tileColor: FloatArray
    private var tileSpeed: Float = 0f

    private var startPositions = mutableListOf<FloatArray>()

    private var tilePositions = mutableListOf<FloatArray>()
    private var tiles = mutableListOf<Tile>()

    private var startTime: Long = 0

    private lateinit var noteTracks: Map<String, NoteTrack>
    private var bpm: Int = 180

    fun passData(noteTracks: Map<String, NoteTrack>, bpm: Int) {
        this.noteTracks = noteTracks
        this.bpm = bpm
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Set color with red, green, blue and alpha (opacity) values
        tileColor = floatArrayOf(0.118f, 0.663f, 0.933f, 1.0f)

        val centerOfTileOnScreenTime = 1500f
        tileSpeed = 2f/centerOfTileOnScreenTime

        val defaultTimeDifference = (60000f)/bpm

        val numberOfColumns = 4
        val tileWidth = 2f / numberOfColumns
//        val tileHeight = (defaultTimeDifference * tileSpeed) * 0.95f // tile should be a little smaller than they could be so they could be distinguishable
        val tileWidthOffset = tileWidth / 2f
//        val tileHeightOffset = tileHeight / 2f

        val startPositionXOffset = -1f + tileWidthOffset
//        val startPositionYOffset = 1f + tileHeightOffset
        for (i in 0..<numberOfColumns){
            val startPosition = floatArrayOf(startPositionXOffset + i*tileWidth, 1f, 0f)
            startPositions.add(startPosition)
        }


        val noteTrackCount = noteTracks.size
        var currentColumn = 1
        for( (noteTrackname, noteTrack) in noteTracks){
            if(currentColumn > numberOfColumns){
                break
            }
            val startPosition = startPositions[currentColumn-1]
            for ( noteDurationTimestamp in noteTrack.noteOnsTimesInMs){
                val (noteStartTime, noteEndTime) = noteDurationTimestamp
                val noteDuration = noteEndTime - noteStartTime
                val noteHeight = (noteDuration.toFloat() * tileSpeed) * 0.95f
                val startPositionYOffset =  noteHeight/2f
                val distance = noteStartTime.toFloat() * tileSpeed
                val tilePosition = startPosition.clone()
                tilePosition[1] += distance + startPositionYOffset
                tilePositions.add(tilePosition)

                val tile = Tile(tileWidth, noteHeight)
                tiles.add(tile)
            }
            currentColumn++
        }

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f, // Camera position
            0f, 0f, 0f, // Camera looks at
            0f, 1.0f, 0.0f) // Camera up

        startTime = SystemClock.uptimeMillis()
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val modelMatrix = FloatArray(16)
        var matrixMVP = FloatArray(16)

        val time = SystemClock.uptimeMillis() - startTime
        val distance = -tileSpeed * time.toInt()

        for((position, tile) in tilePositions.zip(tiles)){
            // Clear Matrix
            Matrix.setIdentityM(modelMatrix, 0)
            // Transpose tile to starting postition
            val (x, y, z) = position
            Matrix.translateM(modelMatrix, 0, x, y, z)
            // Transpose tile down depending on time elapsed
            Matrix.translateM(modelMatrix, 0, 0f, distance, 0f)

            // Combine the rotation matrix with the projection and camera view
            // Note that the vPMatrix factor *must be first* in order
            // for the matrix multiplication product to be correct.
            Matrix.multiplyMM(matrixMVP, 0, matrixVP, 0, modelMatrix, 0)
            tile.draw(matrixMVP, tileColor)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0,
            -1f, 1f,    // left right
            -1f, 1f,    // bottom top
            3f, 7f      // near far
        )
        // Calculate the projection and view transformation
        Matrix.multiplyMM(matrixVP, 0, projectionMatrix, 0, viewMatrix, 0)
    }
}