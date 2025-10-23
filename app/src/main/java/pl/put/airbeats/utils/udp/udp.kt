package pl.put.airbeats.utils.udp

import android.opengl.GLSurfaceView
import android.util.Log
import kotlinx.coroutines.Dispatchers
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicBoolean
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.core.ByteReadPacket
import kotlinx.io.readString

class UdpManager {

    private lateinit var socket: BoundDatagramSocket
    private var remoteAddress: InetAddress? = null
    private var remotePort: Int = 0
    private val isReceiving = AtomicBoolean(false)

    suspend fun connectToServer(ip: String, port: Int): Boolean {
        return try {
            remoteAddress = InetAddress.getByName(ip)
            remotePort = port
            val selectorManager = SelectorManager(Dispatchers.IO)
            socket = aSocket(selectorManager)
                .udp()
                .bind("0.0.0.0", port)
            Log.d("UdpManager", "Connected to $ip:$port")
            val packet = ByteReadPacket("hello".encodeToByteArray())
            val target = InetSocketAddress(ip, port)
            socket.send(
                Datagram(
                    packet,
                    target
                )
            )
            true
        } catch (e: Exception) {
            Log.e("UdpManager", "Connect error: ${e.message}")
            false
        }
    }

    suspend fun startReceivingLoop(glView: GLSurfaceView, onData: (List<String>) -> Unit) {
        isReceiving.set(true)
        while (isReceiving.get()) {
            try {
                val datagram = socket.receive()
                val data = datagram.packet.readString()

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
