package org.obd.graphs.bl.datalogger.connectors

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.IOException
import java.io.OutputStream

private const val LOGGER_TAG = "USB_CONNECTION"
class UsbOutputStream(val port: UsbSerialPort) : OutputStream() {
    override fun write(p0: Int) {
    }

    override fun write(b: ByteArray) {
        try {
            port.write(b, 2 * IO_TIMEOUT)
        } catch (e: IOException) {
            Log.e(LOGGER_TAG, "Failed to write command ${String(b)}", e)
        }
    }
}