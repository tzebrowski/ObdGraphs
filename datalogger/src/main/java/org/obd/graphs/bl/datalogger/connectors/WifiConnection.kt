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


import org.obd.graphs.bl.datalogger.dataLoggerPreferences
import org.obd.metrics.transport.AdapterConnection
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

internal class WifiConnection(private val inetSocketAddress: InetSocketAddress) : AdapterConnection {

    private lateinit var socket: Socket
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    @Throws(IOException::class)
    override fun connect() {
        socket = Socket()
        socket.connect(inetSocketAddress, dataLoggerPreferences.instance.connectionTimeout)
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
        } catch (_: IOException) {
        }
        try {
            outputStream.close()
        } catch (_: IOException) {
        }
        try {
            socket.close()
        } catch (_: IOException) {
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
        fun of(): WifiConnection {
            return WifiConnection(InetSocketAddress(dataLoggerPreferences.instance.tcpHost, dataLoggerPreferences.instance.tcpPort))
        }
    }
}
