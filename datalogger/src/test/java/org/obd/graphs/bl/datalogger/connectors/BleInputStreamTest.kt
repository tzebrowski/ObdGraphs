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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the contract [org.obd.metrics.transport.StreamingConnector] relies on: bytes arrive in
 * order regardless of how notifications were chunked, and a read never blocks forever.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BleInputStreamTest {

    @Test
    fun `serves bytes in order across notification chunks`() {
        val stream = BleInputStream()

        stream.onBytesReceived("41 0C ".toByteArray())
        stream.onBytesReceived("1AF0>".toByteArray())

        assertEquals("41 0C 1AF0>", stream.readAvailable())
    }

    @Test
    fun `returns -1 once closed`() {
        val stream = BleInputStream()
        stream.onBytesReceived("AT".toByteArray())
        stream.close()

        // The buffered bytes are gone with the queue; what matters is that the reader unblocks.
        assertEquals(-1, stream.read())
    }

    @Test
    fun `returns -1 rather than blocking when the adapter goes quiet`() {
        val stream = BleInputStream()

        val elapsed =
            System.currentTimeMillis().let { start ->
                assertEquals(-1, stream.read())
                System.currentTimeMillis() - start
            }

        assertTrue("Expected the read to wait for the timeout, waited ${elapsed}ms", elapsed >= BLE_READ_TIMEOUT_MS)
    }

    @Test
    fun `reports unread bytes of the current chunk as available`() {
        val stream = BleInputStream()
        stream.onBytesReceived("ABC".toByteArray())

        assertEquals('A'.code, stream.read())
        assertEquals(2, stream.available())
    }

    private fun BleInputStream.readAvailable(): String {
        val out = StringBuilder()
        while (true) {
            val next = read()
            if (next == -1) {
                return out.toString()
            }
            out.append(next.toChar())
        }
    }
}
