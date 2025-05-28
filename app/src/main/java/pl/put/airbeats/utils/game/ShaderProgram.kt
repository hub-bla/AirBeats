package pl.put.airbeats.utils.game

import android.opengl.GLES20
import java.nio.FloatBuffer

private const val CORDS_PER_VERTEX = 3
private const val VERTEX_STRIDE = CORDS_PER_VERTEX * 4 // 4 bytes per vertex in float

class ShaderProgram {
    var program: Int

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            // Add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
    init {
        val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """.trimIndent()

        // Load Shaders
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        // Create empty OpenGL ES Program
        program = GLES20.glCreateProgram().also {

            // Add the vertex shader to program
            GLES20.glAttachShader(it, vertexShader)

            // Add the fragment shader to program
            GLES20.glAttachShader(it, fragmentShader)

            // Creates OpenGL ES program executables
            GLES20.glLinkProgram(it)
        }
    }
    fun renderTriangleFan(vertexBuffer: FloatBuffer, vertexCount: Int, mvpMatrix: FloatArray, color: FloatArray) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(program)

        // Get handle to vertex shader's vPosition member
        GLES20.glGetAttribLocation(program, "vPosition").also {

            // Enable a handle to the vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the coordinate data
            GLES20.glVertexAttribPointer(
                it,
                CORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                vertexBuffer
            )

            // Get handle to shape's transformation matrix
            GLES20.glGetUniformLocation(program, "uMVPMatrix").also { matrixHandle ->
                // Pass the transformation matrix to the shader
                GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0)
            }

            // Get handle to fragment shader's vColor member
            GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
                // Pass the color to the shader
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // Draw as triangle fan - pierwszy vertex to centrum, reszta to obwód
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)

            // Disable vertex attribute array
            GLES20.glDisableVertexAttribArray(it)
        }}
        fun render(
            vertexBuffer: FloatBuffer,
            vertexCount: Int,
            mvpMatrix: FloatArray,
            color: FloatArray
        ) {
            // Add program to OpenGL ES environment
            GLES20.glUseProgram(program)

            // Get handle to vertex shader's vPosition member
            GLES20.glGetAttribLocation(program, "vPosition").also {

                // Enable a handle to the vertices
                GLES20.glEnableVertexAttribArray(it)

                // Prepare the coordinate data
                GLES20.glVertexAttribPointer(
                    it,
                    CORDS_PER_VERTEX,
                    GLES20.GL_FLOAT,
                    false,
                    VERTEX_STRIDE,
                    vertexBuffer
                )

                // Get handle to shape's transformation matrix
                GLES20.glGetUniformLocation(program, "uMVPMatrix").also { matrixHandle ->
                    // Pass the transformation matrix to the shader
                    GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0)
                }

                // Get handle to fragment shader's vColor member
                GLES20.glGetUniformLocation(program, "vColor").also { colorHandle ->
                    // Pass the color to the shader
                    GLES20.glUniform4fv(colorHandle, 1, color, 0)
                }

                // Draw the shape
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)

                // Disable vertex array
                GLES20.glDisableVertexAttribArray(it)
            }
        }
    }