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
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

private const val LOGGER_TAG = "BLE_CONNECTION"

/**
 * Read timeout. A quiet link this long is treated as end-of-message.
 *
 * Sized for the SLOWEST thing an ELM327 does, not for a typical PID reply: `ATZ` reboots the
 * adapter, and `AT SP 0` walks every protocol before it answers - both routinely take longer than
 * a few seconds. Cutting a read short there does not just lose that reply: [receive] returns the
 * truncated buffer, the real answer arrives against the NEXT command, and every response from then
 * on is read against the wrong request. That presents as an adapter that connects and then fails
 * to initialise.
 *
 * Nothing pays for the longer wait in the steady state, because every ELM327 reply ends in the '>'
 * prompt and the read returns on it. The timeout only ever fires on genuine silence.
 */
internal const val BLE_READ_TIMEOUT_MS = 12_000L

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
internal class BleInputStream(
    private val readTimeoutMs: Long = BLE_READ_TIMEOUT_MS
) : InputStream() {

    private val queue = LinkedBlockingQueue<ByteArray>()

    private var current: ByteArray = ByteArray(0)
    private var position = 0

    @Volatile
    private var closed = false

    @Volatile
    private var linkLost = false

    /** Called from the GATT callback thread. */
    fun onBytesReceived(bytes: ByteArray) {
        if (!closed && bytes.isNotEmpty()) {
            queue.offer(bytes)
        }
    }

    /**
     * The GATT link dropped while the session was live. This is NOT the same as a quiet line:
     * returning -1 here would look like an ordinary end-of-message to
     * [org.obd.metrics.transport.StreamingConnector], which would keep reading empty responses
     * forever instead of reconnecting. A read has to fail so that the connector sees an IOException.
     */
    fun onLinkLost() {
        linkLost = true
        // Unblocks a reader already parked on the queue.
        queue.offer(POISON_PILL)
    }

    override fun read(): Int {
        if (position < current.size) {
            return current[position++].toInt() and 0xFF
        }

        // A deliberate close wins over a lost link: the caller is shutting down either way.
        if (closed) {
            return -1
        }

        val next =
            try {
                queue.poll(readTimeoutMs, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                null
            }

        if (next == null) {
            failIfLinkLost()
            Log.v(LOGGER_TAG, "No data received within ${readTimeoutMs}ms")
            return -1
        }

        // The pill sits BEHIND whatever had already been notified, so the adapter's last response
        // is still served in full before the failure is raised.
        if (next === POISON_PILL) {
            failIfLinkLost()
            return -1
        }

        current = next
        position = 0
        return read()
    }

    /**
     * Drops everything received so far and reports how many bytes went.
     *
     * Called once, after the CCCD write and before the first command: a lot of adapters push their
     * version banner the moment they are subscribed to. Nothing has been asked for at that point,
     * so those bytes answer no command - but [org.obd.metrics.transport.StreamingConnector] would
     * read them as the reply to the FIRST one, and every response after that is then matched
     * against the wrong request. That presents as an adapter that connects and never initialises.
     *
     * Safe only while no reader is running, which is why it belongs to connect() alone.
     */
    fun discardPending(): Int {
        val dropped = available()

        queue.clear()
        current = ByteArray(0)
        position = 0

        return dropped
    }

    /** Put back so that every later read fails too, not just the one that drained the queue. */
    private fun failIfLinkLost() {
        if (linkLost && !closed) {
            queue.offer(POISON_PILL)
            throw IOException("BLE link to the adapter was lost")
        }
    }

    /** Includes what is still queued, not just the chunk being served. */
    override fun available(): Int = (current.size - position) + queue.sumOf { it.size }

    override fun close() {
        closed = true
        queue.clear()
        queue.offer(POISON_PILL)
    }
}
