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

    var device: String? = null

    fun initBluetooth(btDeviceName: String) {
        this.device = btDeviceName

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
                    Log.i("DATA_LOGGER_BT", "Opened connection to device: $btDeviceName")
                }
            }
        }
        mBluetoothAdapter.cancelDiscovery();
    }

    override fun closeConnection() {
       socket.close();
       Log.i("DATA_LOGGER_BT", "Socket for device: $device is closed.")
    }

    override fun reconnect() {
        socket.close();
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val pairedDevices =
            mBluetoothAdapter.bondedDevices

        for (currentDevice in pairedDevices) {
            if (currentDevice.name.equals(device)) {
                Log.i("DATA_LOGGER_BT", "Reconnecting to the device: $device")
                socket =
                    currentDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                Log.i("DATA_LOGGER_BT", "Get socket for: $device")
                socket.connect()
                Log.i("DATA_LOGGER_BT", "Socket is connected: $device")

                if (socket.isConnected) {
                    input = socket.inputStream
                    output = socket.outputStream
                    Log.i("DATA_LOGGER_BT", "Reconnected to the device: $device")
                }
            }
        }
        connect();
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