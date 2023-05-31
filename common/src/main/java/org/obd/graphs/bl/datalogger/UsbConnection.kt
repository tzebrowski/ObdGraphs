package org.obd.graphs.bl.datalogger


import android.content.Context
import android.hardware.usb.UsbManager
import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val REQUEST_PERMISSIONS_USB = "REQUEST_PERMISSIONS_USB"

private const val LOGGER_TAG = "USB_CONNECTION"

private const val IO_TIMEOUT = 35

private const val MAX_READ_ATTEMPTS = 10

class UsbConnection(val context: Context) : AdapterConnection {

    class UsbInputStream(val port: UsbSerialPort) : InputStream() {
        private var buffer =
            ByteArray(port.readEndpoint.maxPacketSize).apply { fill(0, 0, size) }

        private var readPos = 0
        private var bytesRead = 0

        override fun read(b: ByteArray): Int {
            return port.read(b, IO_TIMEOUT);
        }


        override fun read(): Int {
            if (readPos == 0) {
                buffer = ByteArray(port.readEndpoint.maxPacketSize).apply { fill(0, 0, size) }
                var tmpArray = byteArrayOf()

                loop@ for (it in 1..MAX_READ_ATTEMPTS) {
                    bytesRead = port.read(buffer, IO_TIMEOUT)
                    if (bytesRead > 0) {
                        tmpArray += buffer.copyOfRange(0, bytesRead)
                    }
                    if (bytesRead > 0 && buffer[bytesRead - 1].toInt().toChar() == '>') {
                        break@loop
                    }
                }
                bytesRead = tmpArray.size
                buffer = tmpArray

                if (tmpArray.isEmpty()) {
                    buffer.fill(0, 0, buffer.size)
                    return -1
                }
                return buffer[readPos++].toInt();
            } else {
                return if (readPos < bytesRead && buffer[readPos].toChar() != '>') {
                    buffer[readPos++].toInt()

                } else {
                    readPos = 0
                    -1
                }
            }
        }

        private fun debugRead() {
            Log.e(
                LOGGER_TAG,
                "RX: bytesRead: $bytesRead, buff:  '${
                    String(
                        buffer.copyOfRange(
                            0,
                            bytesRead
                        )
                    )
                }'"
            )
        }
    }

    class UsbOutputStream(val port: UsbSerialPort) : OutputStream() {
        override fun write(p0: Int) {
        }

        override fun write(b: ByteArray) {
            port.write(b, IO_TIMEOUT)
        }
    }


    private lateinit var port: UsbSerialPort
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    @Throws(IOException::class)
    override fun connect() {
        val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager?
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)

        if (availableDrivers.isEmpty()) {
            return
        }

        val driver = availableDrivers[0]
        try {
            Log.e(LOGGER_TAG, "Getting access to the USB device")

            val connection = manager!!.openDevice(driver.device)
                ?: // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                return


            Log.e(LOGGER_TAG, "USB number of ports: ${driver.ports.size}")

            port = driver.ports[0]
            port.open(connection)

            val baudRate = 38400
            port.setParameters(
                baudRate,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            val device = port.device
            Log.e(
                LOGGER_TAG,
                "Allowed to open USB device ${device.deviceId} ${device.deviceName} ${device.deviceProtocol} ${device.deviceClass} " +
                        "${device.manufacturerName} ${device.productId} ${device.serialNumber} ${device.productName}"
            )

            Log.e(LOGGER_TAG, "USB device is opened ${port.isOpen}")
            Log.e(LOGGER_TAG, "Read Endpoint,attributes ${port.readEndpoint.attributes}")
            Log.e(LOGGER_TAG, "Read Endpoint,maxPacketSize ${port.readEndpoint.maxPacketSize}")

        } catch (e: SecurityException) {
            Log.e(LOGGER_TAG, "Failed to access device", e)
//            sendBroadcastEvent(REQUEST_PERMISSIONS_USB)
        }
    }

    @Throws(IOException::class)
    override fun openInputStream(): InputStream {
        return UsbInputStream(port).also { inputStream = it }
    }

    @Throws(IOException::class)
    override fun openOutputStream(): OutputStream {
        return UsbOutputStream(port).also { outputStream = it }
    }

    override fun close() {

        try {
            inputStream.close()
        } catch (e: IOException) {
        }
        try {
            outputStream.close()
        } catch (e: IOException) {
        }
        try {
            port.close()
        } catch (e: IOException) {
        }
    }

    @Throws(IOException::class)
    override fun reconnect() {
        close()
        try {
            Thread.sleep(500)
        } catch (e: InterruptedException) {
        }
        connect()
    }

    companion object {
        fun of(context: Context): UsbConnection {
            return UsbConnection(context)
        }
    }
}
