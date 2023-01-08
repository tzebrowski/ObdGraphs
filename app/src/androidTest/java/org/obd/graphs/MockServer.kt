package org.obd.graphs

import android.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket

private const val LOG_TAG = "MockServer"

class MockServer(
    private val port: Int,
    private val requestResponse: Map<String, String> = mutableMapOf()
) : Runnable {

    private val thread: Thread = Thread(this)
    private var serverSocket: ServerSocket? = null
    private var socket: Socket? = null
    private lateinit var dataInputStream: DataInputStream
    private lateinit var dataOutputStream: DataOutputStream
    private var run: Boolean = true

    fun launch() {
        thread.priority = Thread.NORM_PRIORITY
        thread.start()
    }

    fun stop() {
        run = false
    }

    override fun run() {
        try {
            serverSocket = ServerSocket(port)
            run = true
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Caught IO Exception", e)
        }

        if (waitForClient()) return


        while (run) {

            try {

                val command = readCommand()

                val commandResponse = requestResponse[command]

                Log.i(LOG_TAG, "Received command: $command = $commandResponse")

                if (requestResponse[command] == null) {
                    dataOutputStream.write("$commandResponse >".toByteArray())
                    dataOutputStream.flush()
                } else {
                    dataOutputStream.write("? >".toByteArray())
                    dataOutputStream.flush()
                }

            } catch (e: IOException) {
                Log.e(LOG_TAG, "Caught IO Exception", e)
                break
            }
        }
        Log.i(LOG_TAG, "Finishing Mock Server.")
    }

    private fun waitForClient(): Boolean {
        Log.i(LOG_TAG, "Waiting for upcoming connections")

        try {
            socket = serverSocket?.accept()
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Caught IO Exception", e)
            return true
        }

        Log.i(LOG_TAG, "Client connected")

        try {
            dataInputStream = DataInputStream(BufferedInputStream(socket?.getInputStream()))
            dataOutputStream = DataOutputStream(BufferedOutputStream(socket?.getOutputStream()))
        } catch (e: IOException) {
            Log.e(LOG_TAG, "Caught IO Exception", e)
            return true
        }
        return false
    }

    private fun readCommand(): String {
        var cnt = 0
        var nextByte: Int
        val array = CharArray(255)
        while ((dataInputStream.read().also { nextByte = it } > -1)) {
            array[cnt++] = nextByte.toChar()
            if (nextByte.toChar() == '\r') {
                break
            }
        }

        if (cnt == 0) {
            Log.i(LOG_TAG, "Read no characters")
            return ""
        }
        return array.copyOfRange(0, cnt - 1).concatToString()
    }
}