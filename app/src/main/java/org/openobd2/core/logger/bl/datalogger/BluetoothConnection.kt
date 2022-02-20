package org.openobd2.core.logger.bl.datalogger

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import org.obd.metrics.connection.AdapterConnection
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

private const val LOGGER_TAG = "BluetoothConnection"
internal class BluetoothConnection(btDeviceName: String) : AdapterConnection {

    private val RFCOMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var socket: BluetoothSocket
    private var device: String? = btDeviceName
    private val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    init {
        Log.i(LOGGER_TAG, "Created instance of BluetoothConnection with devices: $btDeviceName")
        if (!mBluetoothAdapter.isEnabled) throw IllegalStateException("Bluetooth stack is disabled")
    }

    override fun reconnect() {
        Log.i(LOGGER_TAG, "Reconnecting to the device: $device")
        input?.close()
        output?.close()
        socket.close()
        TimeUnit.MILLISECONDS.sleep(1000)
        connectToDevice(device)
        Log.i(LOGGER_TAG, "Successfully reconnect to the device: $device")
    }

    override fun connect() {
        connectToDevice(device)
    }

    override fun close() {
        if (::socket.isInitialized)
            socket.close()
        Log.i(LOGGER_TAG, "Socket for device: $device has been closed.")
    }

    override fun openOutputStream(): OutputStream? {
        return output
    }

    override fun openInputStream(): InputStream? {
        return input
    }

    private fun connectToDevice(btDeviceName: String?) {
        Log.i(LOGGER_TAG, "Found bounded connections, size: ${mBluetoothAdapter.bondedDevices.size}")
        for (currentDevice in mBluetoothAdapter.bondedDevices) {
            Log.i(LOGGER_TAG, "Checking bounded connection: ${currentDevice.name} ")

            if (currentDevice.name == btDeviceName) {
                Log.i(LOGGER_TAG, "Bounded connection matches. Opening the device: ${currentDevice.name}")
                socket =
                    currentDevice.createRfcommSocketToServiceRecord(RFCOMM_UUID)
                socket.connect()
                Log.i(LOGGER_TAG, "Doing socket connect for: ${currentDevice.name}")
                if (socket.isConnected) {
                    Log.i(LOGGER_TAG, "Successfully established connection for: ${currentDevice.name}")
                    input = socket.inputStream
                    output = socket.outputStream
                    Log.i(
                        LOGGER_TAG,
                        "Successfully opened  the sockets to device: ${currentDevice.name}"
                    )
                    break
                }
            }
        }
    }
}