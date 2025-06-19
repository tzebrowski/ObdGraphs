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

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import java.io.InputStream

private const val MAX_READ_ATTEMPTS = 7
private const val TERMINATOR_CHAR = '>'
private const val MAX_READ_SIZE = 16 * 1024
private const val LOGGER_TAG = "USB_CONNECTION"

internal class UsbInputStream(
    val port: UsbSerialPort,
) : InputStream() {
    private val buffer =
        ByteArray(MAX_READ_SIZE).apply { fill(0, 0, size) }

    private val tmp =
        ByteArray(MAX_READ_SIZE).apply { fill(0, 0, size) }

    private var buffeReadPos = 0
    private var bytesRead = 0

    override fun read(b: ByteArray): Int = port.read(b, IO_TIMEOUT)

    override fun read(): Int =
        try {
            if (buffeReadPos == 0) {
                fillBuffer()
            } else {
                readFromBuffer()
            }
        } catch (e: java.lang.Exception) {
            Log.i(LOGGER_TAG, "Failed to read data ", e)
            -1
        }

    private fun fillBuffer(): Int {
        var ts = System.currentTimeMillis()
        buffer.run { fill(0, 0, bytesRead) }
        tmp.run { fill(0, 0, size) }

        var nread = 0
        for (it in 1..MAX_READ_ATTEMPTS) {
            bytesRead = port.read(tmp, 0)
            if (bytesRead > 0) {
                System.arraycopy(tmp, 0, buffer, nread, bytesRead)
                nread += bytesRead
                if (buffer[nread - 1].toInt().toChar() == TERMINATOR_CHAR) {
                    break
                }
            }
        }
        bytesRead = nread
        ts = System.currentTimeMillis() - ts
        Log.v(LOGGER_TAG, "Fill buffer time: ${ts}ms")

        if (bytesRead == 0) {
            return -1
        }
        return buffer[buffeReadPos++].toInt()
    }

    private fun readFromBuffer(): Int =
        if (buffeReadPos < bytesRead &&
            buffer[buffeReadPos]
                .toInt()
                .toChar() != TERMINATOR_CHAR
        ) {
            buffer[buffeReadPos++].toInt()
        } else {
            buffeReadPos = 0
            -1
        }
}
