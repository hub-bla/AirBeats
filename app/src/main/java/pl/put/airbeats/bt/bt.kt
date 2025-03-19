package pl.put.airbeats.bt

import android.Manifest
import android.bluetooth.BluetoothAdapter

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothManager {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null


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

    fun receiveData(): String? {
        return try {
            val buffer = ByteArray(1024)
            val bytesRead = inputStream?.read(buffer) ?: -1
            if (bytesRead > 0) {
                String(buffer, 0, bytesRead)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Receive error: ${e.message}")
            null
        }
    }

    fun disconnect() {
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d("BluetoothManager", "Closed.")
        } catch (e: Exception) {
            Log.e("BluetoothManager", "Close error: ${e.message}")
        }
    }
}