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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A BLE write cannot exceed the negotiated MTU, so the chunking here is what keeps long commands
 * from being silently truncated by the stack.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BleOutputStreamTest {

    private val written = mutableListOf<ByteArray>()

    @Test
    fun `splits a command into chunks of the negotiated size`() {
        val stream = streamWith(mtu = 23)

        stream.write(ByteArray(60) { 'A'.code.toByte() })

        assertEquals(listOf(20, 20, 20), written.map { it.size })
    }

    @Test
    fun `sends a short command as a single write`() {
        val stream = streamWith(mtu = 23)

        stream.write("ATZ\r".toByteArray())

        assertEquals(1, written.size)
        assertEquals("ATZ\r", String(written.first()))
    }

    @Test
    fun `uses the larger chunk size once a bigger mtu has been negotiated`() {
        val stream = streamWith(mtu = 517)

        stream.write(ByteArray(600))

        assertEquals(listOf(514, 86), written.map { it.size })
    }

    // An adapter that never agreed an MTU leaves the 20-byte floor every BLE stack supports.
    @Test
    fun `never drops below the default chunk size`() {
        val stream = streamWith(mtu = 0)

        stream.write(ByteArray(30))

        assertEquals(listOf(20, 10), written.map { it.size })
    }

    @Test
    fun `stops writing once a chunk fails`() {
        val stream =
            BleOutputStream(chunkSize = { 20 }, writeChunk = { chunk ->
                written.add(chunk)
                written.size < 2
            })

        stream.write(ByteArray(100))

        assertEquals(2, written.size)
    }

    @Test
    fun `preserves the payload across chunk boundaries`() {
        val stream = streamWith(mtu = 23)
        val command = ByteArray(50) { it.toByte() }

        stream.write(command)

        assertEquals(command.toList(), written.flatMap { it.toList() })
    }

    private fun streamWith(mtu: Int) =
        BleOutputStream(chunkSize = { mtu - 3 }, writeChunk = { chunk ->
            written.add(chunk)
            true
        })
}
