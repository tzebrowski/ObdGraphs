package org.openobd2.core.logger.bl

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import org.openobd2.core.connection.Connection
import java.io.InputStream
import java.io.OutputStream
import java.util.*

internal class BluetoothConnection : Connection {

    var input: InputStream? = null
    var output: OutputStream? = null
    lateinit var socket: BluetoothSocket
    var device: String? = null

    constructor(btDeviceName: String) {
        this.device = btDeviceName
        init(btDeviceName)
    }

    override fun reconnect() {
        Log.i("DATA_LOGGER_BT", "Reconnecting to the device: $device")
        socket.close();
        init(device)
        Log.i("DATA_LOGGER_BT", "Successfully reconnect to the device: $device")
    }


    override fun close() {
        socket.close();
        Log.i("DATA_LOGGER_BT", "Socket for device: $device has been closed.")
    }

    override fun openOutputStream(): OutputStream? {
        return output
    }

    override fun openInputStream(): InputStream? {
        return input
    }

    override fun isClosed(): Boolean {
        return !socket.isConnected
    }


    private fun init(btDeviceName: String?) {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices =
            mBluetoothAdapter.bondedDevices

        for (currentDevice in pairedDevices) {
            if (currentDevice.name.equals(btDeviceName)) {
                Log.i("DATA_LOGGER_BT", "Opening connection to device: $btDeviceName")
                socket =
                    currentDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket.connect()
                if (socket.isConnected) {
                    input = socket.inputStream
                    output = socket.outputStream
                    Log.i(
                        "DATA_LOGGER_BT",
                        "Successfully opened  the connection to device: $btDeviceName"
                    )
                }
            }
        }
        mBluetoothAdapter.cancelDiscovery();
    }
}