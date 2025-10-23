package pl.put.airbeats.utils.udp

import android.opengl.GLSurfaceView
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean

class UdpManager {

    private lateinit var socket: DatagramSocket
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0
    private val isReceiving = AtomicBoolean(false)

    fun connectToServer(ip: String, port: Int): Boolean {
        return try {
            remoteAddress = InetAddress.getByName(ip)
            remotePort = port
            socket = DatagramSocket()
            socket.soTimeout = 0
            Log.d("UdpManager", "Connected to $ip:$port")
            val message = "hello".toByteArray()
            val packet = DatagramPacket(message, message.size, remoteAddress, remotePort)
            socket.send(packet)
            true
        } catch (e: Exception) {
            Log.e("UdpManager", "Connect error: ${e.message}")
            false
        }
    }

    suspend fun startReceivingLoop(glView: GLSurfaceView, onData: (List<String>) -> Unit) {
        withContext(Dispatchers.IO) {
            isReceiving.set(true)
            val buffer = ByteArray(1024)

            while (isReceiving.get()) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    val data = String(packet.data, 0, packet.length)
                    Log.d("UdpManager", "Received: $data")

                    if (data.length == 5 && data[0] in listOf('r', 'l', 'c')) {
                        glView.queueEvent {
                            onData(
                                listOf(
                                    data[0].toString(),
                                    data.slice(1..3),
                                    data[4].toString()
                                )
                            )
                        }
                    } else {
                        Log.w("UdpManager", "Dropped invalid packet: $data")
                    }

                } catch (e: Exception) {
                    Log.e("UdpManager", "Receive loop error: ${e.message}")
                    break
                }
            }
        }
    }

    fun sendMessage(message: String) {
        try {
            val data = message.toByteArray()
            val packet = DatagramPacket(data, data.size, remoteAddress, remotePort)
            socket.send(packet)
            Log.d("UdpManager", "Sent: $message")
        } catch (e: Exception) {
            Log.e("UdpManager", "Send error: ${e.message}")
        }
    }

    fun disconnect() {
        isReceiving.set(false)
        try {
            socket.close()
            Log.d("UdpManager", "Socket closed.")
        } catch (e: Exception) {
            Log.e("UdpManager", "Close error: ${e.message}")
        }
    }
}
