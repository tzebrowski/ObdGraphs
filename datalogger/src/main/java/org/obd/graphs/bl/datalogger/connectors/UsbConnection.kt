 /**
 * Copyright 2019-2025, Tomasz Żebrowski
 *
 * <p>Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.obd.graphs.bl.datalogger.connectors

import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.getS
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

private const val LOGGER_TAG = "USB_CONNECTION"
const val IO_TIMEOUT = 35

data class SerialConnectionSettings(
    val baudRate: Int,
)

internal class UsbConnection(
    val context: Context,
    private val serialConnectionSettings: SerialConnectionSettings,
) : AdapterConnection {
    private lateinit var port: UsbSerialPort
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    @Throws(IOException::class)
    override fun connect() {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager?
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)

        if (availableDrivers.isEmpty()) {
            Log.e(LOGGER_TAG, "No USB devices connected.")
            throw IOException("No USB devices connected")
        }

        try {
            val driver = availableDrivers[0]
            Log.i(LOGGER_TAG, "Getting access to the USB device")

            val connection =
                manager!!.openDevice(driver.device)
                    ?: return

            port = driver.ports[0]
            port.open(connection)

            port.setParameters(
                serialConnectionSettings.baudRate,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            val device = port.device
            Log.i(
                LOGGER_TAG,
                "Allowed to open USB device ${device.deviceId} ${device.deviceName} ${device.deviceProtocol} ${device.deviceClass} " +
                    "${device.manufacturerName} ${device.productId} ${device.serialNumber} ${device.productName}",
            )

            Log.i(LOGGER_TAG, "USB device is opened ${port.isOpen}")
            Log.i(
                LOGGER_TAG,
                "Read Endpoint,attributes ${port.readEndpoint.attributes}",
            )
            Log.i(
                LOGGER_TAG,
                "Read Endpoint,maxPacketSize ${port.readEndpoint.maxPacketSize}",
            )
        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to access device", e)
        }
    }

    @Throws(IOException::class)
    override fun openInputStream(): InputStream? {
        return if (::port.isInitialized) {
            return UsbInputStream(port).also { inputStream = it }
        } else {
            null
        }
    }

    @Throws(IOException::class)
    override fun openOutputStream(): UsbOutputStream? =
        if (::port.isInitialized) {
            UsbOutputStream(port).also { outputStream = it }
        } else {
            null
        }

    override fun close() {
        if (::inputStream.isInitialized) {
            try {
                inputStream.close()
            } catch (_: IOException) {
            }
        }
        if (::outputStream.isInitialized) {
            try {
                outputStream.close()
            } catch (_: IOException) {
            }
        }
        if (::port.isInitialized) {
            try {
                port.close()
            } catch (_: IOException) {
            }
        }
    }

    @Throws(IOException::class)
    override fun reconnect() {
        close()
        try {
            Thread.sleep(500)
        } catch (_: InterruptedException) {
        }
        connect()
    }

    companion object {
        fun of(context: Context): UsbConnection {
            val baudRate = Prefs.getS("pref.adapter.connection.usb.baud_rate", "38400")
            return UsbConnection(context, SerialConnectionSettings(baudRate.toInt()))
        }
    }
}
