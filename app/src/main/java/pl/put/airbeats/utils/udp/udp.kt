package pl.put.airbeats.utils.udp

import android.opengl.GLSurfaceView
import android.util.Log
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicBoolean
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeByte
import kotlinx.io.readByteArray

class UdpManager {

    private lateinit var tcpSocket: Socket
    private lateinit var socket: BoundDatagramSocket
    private lateinit var tcpChannel: ByteWriteChannel
    private val isReceiving = AtomicBoolean(false)
    private val isNotClosed = AtomicBoolean(false)
    private var lastTime: Double = 0.0
    private var lastAngleY: Double = 0.0
//    private var gyrFlag: Boolean = true
    private var baseAngleZ: Double = 0.0
    private val ANGLE_TRESHOLD: Double = 300.0
    private val MIN_RANGE: Double = -45.0
    private val MAX_RANGE: Double = 45.0

    suspend fun connectToServer(ip: String, port: Int): Boolean {
        return try {
            val selectorManager = SelectorManager(Dispatchers.IO)

            if(isNotClosed.get()) {
                tcpChannel.flushAndClose()
                tcpSocket.close()
                socket.close()
            }

            tcpSocket = aSocket(selectorManager)
                .tcp()
                .connect(ip, port)

            tcpChannel = tcpSocket.openWriteChannel(autoFlush = true)

            socket = aSocket(selectorManager)
                .udp()
                .bind(tcpSocket.localAddress)
//                .bind("0.0.0.0", 2138)

            isNotClosed.set(true)
            Log.d("UdpManager", "Connected to $ip:$port")

            tcpChannel.writeByte(1)
            Log.d("UdpManager", "Send 0 to $ip:$port")

            val datagram = socket.receive()
            val data = datagram.packet.readByteArray()
            val isLeftSensor = data[0].toInt() != 0
            val angleX = data.toDoubleLE(1)
            val angleY = data.toDoubleLE(9)
            val angleZ = data.toDoubleLE(17)
            Log.d("UdpManager", "Received ${listOf(isLeftSensor, angleX, angleY, angleZ)}")

            baseAngleZ = angleZ
            lastTime = System.currentTimeMillis() / 1000.0
            lastAngleY = angleY

            true
        } catch (e: Exception) {
            Log.e("UdpManager", "Connect error: ${e.message}")
            false
        }
    }

    suspend fun startReceivingLoop(glView: GLSurfaceView, onData: (Boolean, Double, Int) -> Unit) {
        isReceiving.set(true)
        while (isReceiving.get()) {
            try {
                val datagram = socket.receive()
                val bytes = datagram.packet.readByteArray()

                val (isLeft, pos, columnEvent) = convertControllerData(bytes)
                Log.d("UdpManager", "converted: %s".format(listOf(isLeft, pos, columnEvent).toString()))

                glView.queueEvent {
                    onData(isLeft, pos, columnEvent)
                }

            } catch (e: Exception) {
                Log.e("UdpManager", "Receive loop error: ${e.message}")
                break
            }
        }
        tcpChannel.writeByte(2)
        try {
            tcpChannel.flushAndClose()
            tcpSocket.close()
            socket.close()
            isNotClosed.set(false)
            Log.d("UdpManager", "Socket closed.")
        } catch (e: Exception) {
            Log.e("UdpManager", "Close error: ${e.message}")
        }
    }

    fun ByteArray.toDoubleLE(offset: Int): Double {
        val longBits = (0..7).map { i -> (this[offset + i].toLong() and 0xFF) shl (8 * i) }
            .reduce { acc, v -> acc or v }
        return Double.fromBits(longBits)
    }
    fun convertControllerData(data: ByteArray): Triple<Boolean, Double, Int> {
        val isLeftSensor = data[0].toInt() != 0
        val angleX = data.toDoubleLE(1)
        val angleY = data.toDoubleLE(9)
        val angleZ = data.toDoubleLE(17)
        Log.d("UdpManager", "Received ${listOf(isLeftSensor, angleX, angleY, angleZ)}")

        val currentTime = System.currentTimeMillis() / 1000.0
        val deltaTime = currentTime - lastTime;
        lastTime = currentTime;

        val angleDiff = (angleY - lastAngleY) / deltaTime;
        lastAngleY = angleY;


// Blokuje możliwość szybkiego uderzania (pozostałość po wcześniejszym projekcie)
//        if (!gyrFlag && angleDiff <= ANGLE_TRESHOLD)
//            gyrFlag = true;

        Log.d("UdpManager", "Before shift $angleZ")
        val shifted = wrapAroundBase(angleZ);
        Log.d("UdpManager", "After shift $shifted")
        var columnHit = 9;

        if (/*gyrFlag && */angleDiff > ANGLE_TRESHOLD && angleY > -90.0 && angleY < 90.0) {
//            gyrFlag = false;

            if (shifted < (MIN_RANGE/2.0))
                columnHit = 3; // right outer
            else if (shifted >= (MIN_RANGE/2.0) && shifted < 0.0)
                columnHit = 2; // right inner
            else if (shifted >= 0.0 && shifted < (MAX_RANGE/2.0))
                columnHit = 1; // left inner
            else
                columnHit = 0; // left outer
        }

        val normalized = scale(shifted)
        Log.d("UdpManager", "After scale $normalized")

        return Triple(isLeftSensor, normalized, columnHit )
    }

    fun wrapAroundBase(angleZ: Double): Double {
        // Shifts 0 point to the given angle_z_base without changing domain <-180, 180>
        var yaw = angleZ - baseAngleZ
        if (yaw < -180) yaw += 360
        if (yaw > 180) yaw -= 360
        return yaw;
    }

    fun scale(angle: Double): Double {
        val normalized = ( (-angle-MIN_RANGE) / (MAX_RANGE-MIN_RANGE) ) * 2.0
        val scaled = normalized.coerceIn(0.0, 2.0)
        return scaled - 1.0
//        var clamped = (-angle).coerceIn(MIN_RANGE, MAX_RANGE)
//        clamped += MAX_RANGE;
//        clamped *= 180f / MAX_RANGE;
//        //clamped /= MAX_RANGE - MIN_RANGE; // normalized [1,0]
//        return "%03d".format(clamped.toInt())
    }

    fun disconnect() {
        isReceiving.set(false)
    }
}

data class UdpData(
    val enabled: Boolean,
    val x: Float,
    val y: Float,
    val z: Float
)
