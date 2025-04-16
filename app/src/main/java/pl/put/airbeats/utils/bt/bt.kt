package pl.put.airbeats.utils.bt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

class BluetoothManager() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var receivingThread: Thread? = null
    private val isReceiving = AtomicBoolean(false)

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectToDevice(deviceName: String): Boolean {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            Log.e("BluetoothManager", "BT unavailable")
            return false
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        val device = pairedDevices.find { it.name == deviceName }
        if (device != null) {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
                bluetoothSocket?.connect()
                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream
                Log.d("BluetoothManager", "Connected: $deviceName")
                return true
            } catch (e: Exception) {
                Log.e("BluetoothManager", "Connect error: ${e.message}")
                return false
            }
        } else {
            Log.e("BluetoothManager", "Not Found: $deviceName")
            return false
        }
    }

    fun startReceivingLoop(glView:GLSurfaceView, onData: (String) -> Unit) {
        isReceiving.set(true)
        receivingThread = Thread {
            val buffer = ByteArray(1024)
            while (isReceiving.get()) {
                try {
                    val bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val data = String(buffer, 0, bytesRead)
                        Log.d("BluetoothManager", "Received: $data")
                        // glEventLoop Pipe
                        glView.queueEvent {
                            onData(data)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BluetoothManager", "Receive loop error: ${e.message}")
                    break
                }
            }
        }
        receivingThread?.start()
    }

    fun disconnect() {
        isReceiving.set(false)
        try {
            receivingThread?.interrupt()
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d("BluetoothManager", "Closed.")
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Close error: ${e.message}")
        }
    }
}
