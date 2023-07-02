package org.obd.graphs.bl.datalogger.connectors

import android.bluetooth.BluetoothSocket
import android.util.Log
import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.graphs.network
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import java.util.concurrent.TimeUnit

private const val LOGGER_TAG = "BluetoothConnection"
private val RFCOMM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

class BluetoothConnection(deviceName: String) : AdapterConnection {

    private var input: InputStream? = null
    private var output: OutputStream? = null
    private lateinit var socket: BluetoothSocket
    private var device: String? = deviceName

    init {
        Log.i(LOGGER_TAG, "Created instance of BluetoothConnection with devices: $deviceName")
    }

    override fun reconnect() {
        Log.i(LOGGER_TAG, "Reconnecting to the device: $device")
        if (dataLoggerPreferences.instance.reconnectWhenError && dataLoggerPreferences.instance .hardReset) {
            throw IOException("Doing hard reset")
        }

        close()

        TimeUnit.MILLISECONDS.sleep(1000)
        connectToDevice(device)
        Log.i(LOGGER_TAG, "Successfully reconnect to the device: $device")
    }

    override fun connect() {
        connectToDevice(device)
    }

    override fun close() {

        try {
            input?.close()
        } catch (_: Throwable){}

        try {

            output?.close()
        } catch (_: Throwable){}

        try {
            if (::socket.isInitialized)
                socket.close()
        } catch (_: Throwable){}

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
                "Found bounded connections, size: ${network.bluetoothAdapter()?.bondedDevices?.size}"
            )
            btDeviceName?.let {
                network.findBluetoothAdapterByName(it)?.let { adapter ->
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
            network.requestBluetoothPermissions()
        }
    }
}