/*
 * Copyright 2019-2026, Tomasz Żebrowski
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
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val LOGGER_TAG = "BLE_CONNECTION"

/** Read timeout. A quiet link this long is treated as end-of-message, exactly as USB does. */
internal const val BLE_READ_TIMEOUT_MS = 3000L

/** Handed to the queue on close so a blocked reader unblocks instead of waiting out the timeout. */
private val POISON_PILL = ByteArray(0)

/**
 * Serves bytes received as GATT notifications to [org.obd.metrics.transport.StreamingConnector],
 * which reads BYTE BY BYTE and stops on '>' or -1.
 *
 * Framing is deliberately not done here: notifications split and coalesce arbitrarily, so locating
 * the prompt character is the connector's job. This class only has to deliver bytes in order and
 * return -1 rather than block forever.
 */
internal class BleInputStream : InputStream() {

    private val queue = LinkedBlockingQueue<ByteArray>()

    private var current: ByteArray = ByteArray(0)
    private var position = 0

    @Volatile
    private var closed = false

    /** Called from the GATT callback thread. */
    fun onBytesReceived(bytes: ByteArray) {
        if (!closed && bytes.isNotEmpty()) {
            queue.offer(bytes)
        }
    }

    override fun read(): Int {
        if (position < current.size) {
            return current[position++].toInt() and 0xFF
        }

        if (closed) {
            return -1
        }

        val next =
            try {
                queue.poll(BLE_READ_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            }

        if (next == null) {
            Log.v(LOGGER_TAG, "No data received within ${BLE_READ_TIMEOUT_MS}ms")
            return -1
        }

        if (next === POISON_PILL) {
            return -1
        }

        current = next
        position = 0
        return read()
    }

    override fun available(): Int = current.size - position

    override fun close() {
        closed = true
        queue.clear()
        queue.offer(POISON_PILL)
    }
}
