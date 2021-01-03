package org.openobd2.core.logger.bl

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import org.openobd2.core.channel.Channel
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*

internal class BluetoothChannelmpl : Channel() {

    var input: InputStream? = null
    var output: OutputStream? = null
    lateinit var socket: BluetoothSocket

    fun initBluetooth(btDeviceName: String) {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices =
            mBluetoothAdapter.bondedDevices

        for (currentDevice in pairedDevices) {
            if (currentDevice.name.equals(btDeviceName)) {
                Log.i("DATA_LOGGER_BT", "Found device: $btDeviceName")
                socket =
                    currentDevice.createRfcommSocketToServiceRecord( UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket.connect()
                if (socket.isConnected) {
                    input = socket.inputStream
                    output = socket.outputStream
                    Log.i("DATA_LOGGER_BT", "Opened connection to device: $btDeviceName")
                }
            }
        }
    }

    override fun getOutputStream(): OutputStream? {
        return output
    }

    override fun getInputStream(): InputStream? {
        return input
    }

    override fun isClosed(): Boolean {
        return !socket.isConnected
    }
}