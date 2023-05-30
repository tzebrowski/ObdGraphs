package org.obd.graphs.bl.datalogger


import android.content.Context
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import org.obd.graphs.sendBroadcastEvent
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

const val REQUEST_PERMISSIONS_USB = "REQUEST_PERMISSIONS_USB"

class UsbConnection(val context: Context) : AdapterConnection {

    class UsbInputStream (val port: UsbSerialPort) : InputStream() {
        override fun read(b: ByteArray?): Int {
            return port.read(b,200);
        }
        override fun read(): Int {
            return 0;
        }
    }

    class UsbOutputStream (val port: UsbSerialPort) : OutputStream() {
        override fun write(p0: Int) {
        }

        override fun write(b: ByteArray?) {
            port.write(b,200)
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
            val connection = manager!!.openDevice(driver.device)
                ?: // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                return

            port = driver.ports[0]
            port.open(connection)
            port.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        } catch (e: SecurityException) {
            sendBroadcastEvent(REQUEST_PERMISSIONS_USB)
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
