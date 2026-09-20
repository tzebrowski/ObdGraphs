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
import java.io.OutputStream

private const val LOGGER_TAG = "BLE_CONNECTION"

/**
 * The floor every BLE stack supports: the default 23-byte MTU minus the 3-byte ATT header. Used
 * until the adapter agrees to a larger MTU.
 */
internal const val BLE_DEFAULT_CHUNK_SIZE = 20

/**
 * Writes commands to the adapter's write characteristic.
 *
 * A BLE write cannot exceed the negotiated MTU, so a command is split into chunks, and the chunks
 * are sent one at a time - GATT permits a single outstanding operation per connection, and firing
 * the next write before the previous one completes drops it silently.
 *
 * [writeChunk] performs one GATT write and waits for its completion; it is supplied by
 * [BleConnection], which owns the callback plumbing.
 */
internal class BleOutputStream(
    private val chunkSize: () -> Int,
    private val writeChunk: (ByteArray) -> Boolean
) : OutputStream() {

    override fun write(p0: Int) {
        write(byteArrayOf(p0.toByte()))
    }

    override fun write(b: ByteArray) {
        val size = chunkSize().coerceAtLeast(BLE_DEFAULT_CHUNK_SIZE)

        var offset = 0
        while (offset < b.size) {
            val end = minOf(offset + size, b.size)
            val chunk = b.copyOfRange(offset, end)

            if (!writeChunk(chunk)) {
                Log.e(LOGGER_TAG, "Failed to write command ${String(b)}")
                return
            }
            offset = end
        }
    }
}
