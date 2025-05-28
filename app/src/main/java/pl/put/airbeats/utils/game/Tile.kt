package pl.put.airbeats.utils.game

import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

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

    fun draw(x: Float, y: Float, z: Float) {
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, position, 0)
        shaderProgram.render(vertexBuffer, VERTEX_COUNT, mvpMatrix, color)
    }

    fun draw(moveDirection: FloatArray) {
        val (x, y, z) = moveDirection
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, position, 0)
        shaderProgram.render(vertexBuffer, VERTEX_COUNT, mvpMatrix, color)
    }
}

class Dot {
    private val shaderProgram: ShaderProgram

    val tileHeight: Float

    private val circleCords: FloatArray
    private val vertexBuffer: FloatBuffer
    private val vertexCount: Int

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
        segments: Int = 32 // Number of segments for circle smoothness
    ) {
        this.shaderProgram = shaderProgram
        this.tileHeight = tileHeight

        // Generate circle vertices
        val circleData = calculateCircleCords(tileWidth, tileHeight, segments)
        this.circleCords = circleData.first
        this.vertexCount = circleData.second

        this.vertexBuffer = ByteBuffer.allocateDirect(circleCords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(circleCords)
                position(0)
            }
        }

        this.position = if (position.isEmpty()) {
            FloatArray(16).also { Matrix.setIdentityM(it, 0) }
        } else {
            position.copyOf()
        }

        this.vpMatrix = vpMatrix
        this.mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(this.mvpMatrix, 0, this.vpMatrix, 0, this.position, 0)

        this.color = color
    }

    private fun calculateCircleCords(tileWidth: Float, tileHeight: Float, segments: Int): Pair<FloatArray, Int> {
        val radiusX = tileWidth / 2f
        val radiusY = tileHeight / 2f

        // Create vertices for triangle fan (center + perimeter points)
        val vertices = mutableListOf<Float>()

        // Center vertex
        vertices.addAll(listOf(0.0f, 0.0f, 0.0f))

        // Perimeter vertices
        for (i in 0..segments) {
            val angle = 2.0f * Math.PI.toFloat() * i / segments
            val x = radiusX * cos(angle)
            val y = radiusY * sin( angle)
            vertices.addAll(listOf(x, y, 0.0f))
        }

        return Pair(vertices.toFloatArray(), segments + 2) // +1 for center, +1 for closing the circle
    }

    // Alternative: Generate triangles instead of triangle fan
    private fun calculateCircleCordsTriangles(tileWidth: Float, tileHeight: Float, segments: Int): Pair<FloatArray, Int> {
        val radiusX = tileWidth / 2f
        val radiusY = tileHeight / 2f

        val vertices = mutableListOf<Float>()

        for (i in 0 until segments) {
            val angle1 = 2.0f * Math.PI.toFloat() * i / segments
            val angle2 = 2.0f * Math.PI.toFloat() * (i + 1) / segments

            // Center vertex
            vertices.addAll(listOf(0.0f, 0.0f, 0.0f))

            // First perimeter vertex
            val x1 = radiusX * cos(angle1)
            val y1 = radiusY * sin(angle1)
            vertices.addAll(listOf(x1, y1, 0.0f))

            // Second perimeter vertex
            val x2 = radiusX * cos(angle2)
            val y2 = radiusY * sin(angle2)
            vertices.addAll(listOf(x2, y2, 0.0f))
        }

        return Pair(vertices.toFloatArray(), segments * 3)
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
        val worldPos = floatArrayOf(0f, 0f, 0f, 1f)
        val result = FloatArray(4)
        Matrix.multiplyMV(result, 0, mvpMatrix, 0, worldPos, 0)
        return if (result[3] != 0f) {
            floatArrayOf(result[0]/result[3], result[1]/result[3], result[2]/result[3], 1f)
        } else {
            result
        }
    }

    fun draw() {

        shaderProgram.renderTriangleFan(vertexBuffer, vertexCount, mvpMatrix, color)

    }

    fun draw(x: Float, y: Float, z: Float) {
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, position, 0)
        shaderProgram.renderTriangleFan(vertexBuffer, vertexCount, mvpMatrix, color)
    }

    fun draw(moveDirection: FloatArray) {
        val (x, y, z) = moveDirection
        Matrix.setIdentityM(position, 0)
        Matrix.translateM(position, 0, x, y, z)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, position, 0)
        shaderProgram.renderTriangleFan(vertexBuffer, vertexCount, mvpMatrix, color)
    }
}