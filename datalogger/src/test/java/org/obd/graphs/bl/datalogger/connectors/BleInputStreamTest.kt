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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

// Keeps the suite fast: these cases exercise ordering and failure, never the real wait.
private const val TEST_READ_TIMEOUT_MS = 200L

/**
 * Guards the contract [org.obd.metrics.transport.StreamingConnector] relies on: bytes arrive in
 * order regardless of how notifications were chunked, and a read never blocks forever.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BleInputStreamTest {

    @Test
    fun `serves bytes in order across notification chunks`() {
        val stream = BleInputStream(readTimeoutMs = TEST_READ_TIMEOUT_MS)

        stream.onBytesReceived("41 0C ".toByteArray())
        stream.onBytesReceived("1AF0>".toByteArray())

        assertEquals("41 0C 1AF0>", stream.readAvailable())
    }

    @Test
    fun `returns -1 once closed`() {
        val stream = BleInputStream(readTimeoutMs = TEST_READ_TIMEOUT_MS)
        stream.onBytesReceived("AT".toByteArray())
        stream.close()

        // The buffered bytes are gone with the queue; what matters is that the reader unblocks.
        assertEquals(-1, stream.read())
    }

    @Test
    fun `returns -1 rather than blocking when the adapter goes quiet`() {
        // The production timeout is sized for an ELM327 reboot; the test only needs the behaviour.
        val timeout = TEST_READ_TIMEOUT_MS
        val stream = BleInputStream(readTimeoutMs = timeout)

        val elapsed =
            System.currentTimeMillis().let { start ->
                assertEquals(-1, stream.read())
                System.currentTimeMillis() - start
            }

        assertTrue("Expected the read to wait for the timeout, waited ${elapsed}ms", elapsed >= timeout)
    }

    // ELM327 reboots and protocol searches routinely outrun a few seconds. A read that gives up
    // first hands StreamingConnector a truncated reply, and every later response is then matched
    // against the wrong command.
    @Test
    fun `waits long enough for the slowest ELM327 command`() {
        assertTrue("BLE read timeout is too short for ATZ / AT SP 0", BLE_READ_TIMEOUT_MS >= 10_000L)
    }

    // A dropped link must NOT look like an ordinary end-of-message, or StreamingConnector keeps
    // reading empty responses instead of reconnecting.
    @Test
    fun `fails the read once the link is lost`() {
        val stream = BleInputStream(readTimeoutMs = TEST_READ_TIMEOUT_MS)
        stream.onLinkLost()

        assertThrows(IOException::class.java) { stream.read() }
    }

    @Test
    fun `serves buffered bytes before failing on a lost link`() {
        val stream = BleInputStream(readTimeoutMs = TEST_READ_TIMEOUT_MS)
        stream.onBytesReceived("OK".toByteArray())
        stream.onLinkLost()

        assertEquals('O'.code, stream.read())
        assertEquals('K'.code, stream.read())
        assertThrows(IOException::class.java) { stream.read() }
    }

    // A deliberate close is a shutdown, not a fault - it still ends the stream quietly.
    @Test
    fun `returns -1 when closed after the link was lost`() {
        val stream = BleInputStream(readTimeoutMs = TEST_READ_TIMEOUT_MS)
        stream.onLinkLost()
        stream.close()

        assertEquals(-1, stream.read())
    }

    @Test
    fun `reports unread bytes of the current chunk as available`() {
        val stream = BleInputStream(readTimeoutMs = TEST_READ_TIMEOUT_MS)
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
