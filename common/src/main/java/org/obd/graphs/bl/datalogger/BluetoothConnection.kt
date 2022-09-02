package org.obd.graphs.bl.datalogger

import android.bluetooth.BluetoothSocket
import android.util.Log
import org.obd.graphs.*
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

private const val LOGGER_TAG = "BluetoothConnection"
private val RFCOMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

internal class BluetoothConnection(btDeviceName: String) : AdapterConnection {

    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var socket: BluetoothSocket
    private var device: String? = btDeviceName

    val preferences: DataLoggerPreferences by lazy { DataLoggerPreferences.instance }

    init {
        Log.i(LOGGER_TAG, "Created instance of BluetoothConnection with devices: $btDeviceName")
    }

    override fun reconnect() {
        Log.i(LOGGER_TAG, "Reconnecting to the device: $device")
        if (preferences.reconnectWhenError && preferences.hardReset) {
            throw IOException("Doing hard reset")
        }
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
        try {
            Log.i(
                LOGGER_TAG,
                "Found bounded connections, size: ${bluetoothAdapter()?.bondedDevices?.size}"
            )
            btDeviceName?.let {
                findBluetoothAdapterByName(it)?.let { adapter ->
                    Log.i(
                        LOGGER_TAG,
                        "Opening connection to bounded device: ${adapter.name}"
                    )
                    socket =
                        adapter.createRfcommSocketToServiceRecord(RFCOMM_UUID)
                    socket.connect()
                    Log.i(LOGGER_TAG, "Doing socket connect for: ${adapter.name}")

                    if (socket.isConnected) {
                        Log.i(
                            LOGGER_TAG,
                            "Successfully established connection for: ${adapter.name}"
                        )
                        input = socket.inputStream
                        output = socket.outputStream
                        Log.i(
                            LOGGER_TAG,
                            "Successfully opened  the sockets to device: ${adapter.name}"
                        )
                    }
                }
            }
        }catch (e: SecurityException){
            requestBluetoothPermissions()
        }
    }
}