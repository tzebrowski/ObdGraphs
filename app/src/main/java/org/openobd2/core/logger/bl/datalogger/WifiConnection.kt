package org.openobd2.core.logger.bl.datalogger


import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class WifiConnection(private val inetSocketAddress: InetSocketAddress) : AdapterConnection {

    private lateinit var socket: Socket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    @Throws(IOException::class)
    override fun connect() {
        socket = Socket()
        socket.connect(inetSocketAddress)
    }

    @Throws(IOException::class)
    override fun openInputStream(): InputStream {
        return socket.getInputStream().also { inputStream = it }
    }

    @Throws(IOException::class)
    override fun openOutputStream(): OutputStream {
        return socket.getOutputStream().also { outputStream = it }
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
            socket.close()
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
        fun of(host: String?, port: Int): WifiConnection {
            return WifiConnection(InetSocketAddress(host, port))
        }
    }
}
