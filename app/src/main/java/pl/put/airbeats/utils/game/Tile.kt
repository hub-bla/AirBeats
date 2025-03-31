package pl.put.airbeats.utils.game

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

const val CORDS_PER_VERTEX = 3

class Tile(tileWidth: Float, tileHeight: Float) {
    private val squareCords: FloatArray

    init {
        val centerOffsetX = tileWidth/2f
        val centerOffsetY = tileHeight/2f
        val leftX = -centerOffsetX
        val rightX = centerOffsetX
        val topY = centerOffsetY
        val bottomY = -centerOffsetY
        squareCords = floatArrayOf(
            leftX,  topY,   0.0f,      // top left
            leftX,  bottomY,0.0f,      // bottom left
            rightX, bottomY,0.0f,      // bottom right
            leftX,  topY,   0.0f,      // top left
            rightX, bottomY,0.0f,      // bottom right
            rightX, topY,   0.0f       // top right
        )
    }

    // initialize vertex byte buffer for shape coordinates
    private val vertexBuffer: FloatBuffer =
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(squareCords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(squareCords)
                position(0)
            }
        }

    private val vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        // the matrix must be included as a modifier of gl_Position
        // Note that the uMVPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}"

    fun loadShader(type: Int, shaderCode: String): Int {

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        return GLES20.glCreateShader(type).also { shader ->

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }

    private var mProgram: Int

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // create empty OpenGL ES Program
        mProgram = GLES20.glCreateProgram().also {

            // add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }

    // Use to access and set the view transformation
    private var vMVPMatrixHandle: Int = 0
    private var positionHandle: Int = 0
    private var mColorHandle: Int = 0

    private val vertexCount: Int = squareCords.size / CORDS_PER_VERTEX
    private val vertexStride: Int = CORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mvpMatrix: FloatArray, color: FloatArray) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the coordinate data
            GLES20.glVertexAttribPointer(
                it,
                CORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // get handle to shape's transformation matrix
            vMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(vMVPMatrixHandle, 1, false, mvpMatrix, 0)

            // Draw the triangle
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }
}