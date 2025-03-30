package pl.put.airbeats.utils.game

import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock

class MyGLRenderer : GLSurfaceView.Renderer {
    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val VPMatrix = FloatArray(16)

    private lateinit var color1: FloatArray
    private lateinit var color2: FloatArray
    private lateinit var color3: FloatArray
    private lateinit var color4: FloatArray

    private lateinit var startPos1: FloatArray
    private lateinit var startPos2: FloatArray
    private lateinit var startPos3: FloatArray
    private lateinit var startPos4: FloatArray

    private lateinit var mTile1: Tile
    private lateinit var mTile2: Tile
    private lateinit var mTile3: Tile
    private lateinit var mTile4: Tile

    private var startTime: Long = 0

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Set color with red, green, blue and alpha (opacity) values
        color1 = floatArrayOf(0.118f, 0.663f, 0.933f, 1.0f)
        color2 = floatArrayOf(0.118f, 0.663f, 0.833f, 1.0f)
        color3 = floatArrayOf(0.118f, 0.663f, 0.733f, 1.0f)
        color4 = floatArrayOf(0.118f, 0.663f, 0.633f, 1.0f)

        startPos1 = floatArrayOf(-0.75f, 0.75f, 0f, 0f)
        startPos2 = floatArrayOf(-0.25f, 0.85f, 0f, 0f)
        startPos3 = floatArrayOf(0.25f, 0.95f, 0f, 0f)
        startPos4 = floatArrayOf(0.75f, 1.05f, 0f, 0f)

        // initialize tiles
        mTile1 = Tile()
        mTile2 = Tile()
        mTile3 = Tile()
        mTile4 = Tile()

        startTime = SystemClock.uptimeMillis()
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0,
            0f, 0f, 3f, // Camera position
            0f, 0f, 0f, // Camera looks at
            0f, 1.0f, 0.0f) // Camera up

        // Calculate the projection and view transformation
        Matrix.multiplyMM(VPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val modelMatrix = FloatArray(16)
        val MVPMatrix = FloatArray(16)

        val time = SystemClock.uptimeMillis() - startTime
//        val angle = 0.090f * time.toInt()
        val distance = -0.0005f * time.toInt()
        val transposeArray = floatArrayOf(0f, -distance, 0f, 0f)

        // Clear Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        // Transpose tile to top of view
        Matrix.translateM(modelMatrix, 0, startPos1[0],startPos1[1],startPos1[2])
        // Transpose tile down to depending on time elapsed
        Matrix.translateM(modelMatrix, 0, 0f, distance, 0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(MVPMatrix, 0, VPMatrix, 0, modelMatrix, 0)
        mTile1.draw(MVPMatrix, color1)

        // Clear Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        // Transpose tile to top of view
        Matrix.translateM(modelMatrix, 0, startPos2[0],startPos2[1],startPos2[2])
        // Transpose tile down to depending on time elapsed
        Matrix.translateM(modelMatrix, 0, 0f, distance, 0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(MVPMatrix, 0, VPMatrix, 0, modelMatrix, 0)
        mTile2.draw(MVPMatrix, color2)

        // Clear Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        // Transpose tile to top of view
        Matrix.translateM(modelMatrix, 0, startPos3[0],startPos3[1],startPos3[2])
        // Transpose tile down to depending on time elapsed
        Matrix.translateM(modelMatrix, 0, 0f, distance, 0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(MVPMatrix, 0, VPMatrix, 0, modelMatrix, 0)
        mTile3.draw(MVPMatrix, color3)

        // Clear Matrix
        Matrix.setIdentityM(modelMatrix, 0)
        // Transpose tile to top of view
        Matrix.translateM(modelMatrix, 0, startPos4[0],startPos4[1],startPos4[2])
        // Transpose tile down to depending on time elapsed
        Matrix.translateM(modelMatrix, 0, 0f, distance, 0f)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(MVPMatrix, 0, VPMatrix, 0, modelMatrix, 0)
        mTile4.draw(MVPMatrix, color4)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0,
            -1f, 1f, // left right
            -1f, 1f, // bottom top
            3f, 7f) // near far
    }
}