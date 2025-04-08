package pl.put.airbeats.utils.game

import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val VERTEX_COUNT = 6

class Tile {
    private val shaderProgram: ShaderProgram

    val tileHeight: Float

    private val squareCords: FloatArray
    private val vertexBuffer: FloatBuffer

    private val position: FloatArray
    private var vpMatrix: FloatArray
    private val mvpMatrix: FloatArray

    private var color: FloatArray

    constructor(
        shaderProgram: ShaderProgram,
        tileWidth: Float = 1f,
        tileHeight: Float = 1f,
        position: FloatArray = floatArrayOf(),
        vpMatrix: FloatArray = FloatArray(16),
        color: FloatArray = floatArrayOf(0.118f, 0.663f, 0.933f, 1.0f),
    ) {
        this.shaderProgram = shaderProgram

        this.tileHeight = tileHeight

        this.squareCords = calculateSquareCords(tileWidth, tileHeight)
        this.vertexBuffer = ByteBuffer.allocateDirect(squareCords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(squareCords)
                position(0)
            }
        }

        this.position = position.copyOf()
        if(this.position.isEmpty()) {
            Matrix.setIdentityM(this.position, 0)
        }
        this.vpMatrix = vpMatrix
        this.mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(this.mvpMatrix, 0, this.vpMatrix, 0, this.position, 0)

        this.color = color
    }

    private fun calculateSquareCords(tileWidth: Float, tileHeight: Float): FloatArray {
        val centerOffsetX = tileWidth/2f
        val centerOffsetY = tileHeight/2f
        val leftX = -centerOffsetX
        val rightX = centerOffsetX
        val topY = centerOffsetY
        val bottomY = -centerOffsetY
        return floatArrayOf(
            leftX,  topY,   0.0f,      // top left
            leftX,  bottomY,0.0f,      // bottom left
            rightX, bottomY,0.0f,      // bottom right
            leftX,  topY,   0.0f,      // top left
            rightX, bottomY,0.0f,      // bottom right
            rightX, topY,   0.0f       // top right
        )
    }

    fun move(moveDirection: FloatArray) {
        val (x, y, z) = moveDirection
        Matrix.translateM(position, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, position, 0)
    }

    fun move(x: Float, y: Float, z: Float) {
        Matrix.translateM(position, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, position, 0)
    }

    fun changeColor(newColor: FloatArray) {
        color = newColor
    }

    fun getPosition(): FloatArray {
        val position = floatArrayOf(0f,0f,0f,1f)
        Matrix.multiplyMV(position, 0, mvpMatrix, 0, position, 0)
        val (x,y,z,w) = position
        return floatArrayOf(x/w, y/w, z/w, 1f)
    }


    fun draw() {
        shaderProgram.render(vertexBuffer, VERTEX_COUNT, mvpMatrix, color)
    }
}