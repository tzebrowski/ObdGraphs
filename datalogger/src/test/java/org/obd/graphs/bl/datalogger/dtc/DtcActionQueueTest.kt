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
package org.obd.graphs.bl.datalogger.dtc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.metrics.api.model.DtcAction
import org.obd.metrics.api.model.Init

class DtcActionQueueTest {
    private var time = 0L
    private val queue = DtcActionQueue { time }

    private val modules =
        listOf("Engine ECU", "ABS").map { Init.Header.builder().mode(it).header("DA10F1").build() }

    private val connectScan = DtcRequest(DtcScanKind.CONNECT, modules)
    private val clear = DtcRequest(DtcScanKind.CLEAR, modules)
    private val read = DtcRequest(DtcScanKind.READ, modules)

    @Test
    fun `clear pressed during the connect scan waits, and each completion keeps its own request`() {
        // As logged on the car: connect scan running, Clear pressed. Before, the scan's result was taken for
        // the clear's, and the clear's read was stored with no modules.
        assertTrue(queue.submit(connectScan))
        assertFalse(queue.submit(clear))

        val first = queue.complete()
        assertEquals(connectScan, first.finished)
        assertEquals(clear, first.next)

        val second = queue.complete()
        assertEquals(clear, second.finished)
        assertEquals(listOf("Engine ECU", "ABS"), second.finished!!.moduleLabels)
        assertNull(second.next)
    }

    // For the shared report: from when the request was sent to its completion, not from when it was submitted.
    @Test
    fun `a completion carries how long its request ran, a waiting request's time starts when it is sent`() {
        time = 1_000
        queue.submit(connectScan)
        time = 3_000
        queue.submit(clear)
        time = 14_500
        assertEquals(13_500L, queue.complete().durationMs)
        time = 20_000
        assertEquals(5_500L, queue.complete().durationMs)
        assertNull(queue.complete().durationMs)
    }

    @Test
    fun `a read does not replace a waiting clear, a newer clear does`() {
        queue.submit(connectScan)
        queue.submit(clear)
        queue.submit(read)
        assertEquals(clear, queue.complete().next)

        val newerClear = DtcRequest(DtcScanKind.CLEAR)
        queue.submit(newerClear)
        assertEquals(newerClear, queue.complete().next)
    }

    @Test
    fun `the newest waiting read wins`() {
        queue.submit(connectScan)
        queue.submit(read)
        val newer = DtcRequest(DtcScanKind.READ)
        queue.submit(newer)

        assertEquals(newer, queue.complete().next)
    }

    @Test
    fun `an action without a completion stops blocking after its timeout`() {
        queue.submit(connectScan)
        time = dtcActionTimeoutMs(modules.size) + 1

        assertTrue(queue.submit(read))
        assertEquals(read, queue.complete().finished)
    }

    @Test
    fun `a completion nobody asked for has no request`() {
        assertEquals(DtcCompletion(null, null), queue.complete())
    }

    @Test
    fun `reset drops the action in flight and the waiting one`() {
        queue.submit(connectScan)
        queue.submit(clear)
        queue.reset()

        assertTrue(queue.submit(read))
        assertEquals(DtcCompletion(read, null, durationMs = 0), queue.complete())
    }

    @Test
    fun `the picker's freeze frame choice beats the setting`() {
        assertEquals(DtcAction.READ_SNAPSHPOTS, DtcRequest(DtcScanKind.READ, snapshots = true).readAction(snapshotsSetting = false))
        assertEquals(DtcAction.READ, DtcRequest(DtcScanKind.READ, snapshots = false).readAction(snapshotsSetting = true))
    }

    @Test
    fun `without a choice the setting decides`() {
        assertEquals(DtcAction.READ_SNAPSHPOTS, DtcRequest(DtcScanKind.CONNECT).readAction(snapshotsSetting = true))
        assertEquals(DtcAction.READ, DtcRequest(DtcScanKind.CLEAR).readAction(snapshotsSetting = false))
    }

    @Test
    fun `after a connect from the dialog a read runs only if the connect did not read`() {
        assertTrue(dtcRunsAfterConnect(isRead = true, readsOnConnect = false))
        assertFalse(dtcRunsAfterConnect(isRead = true, readsOnConnect = true))
    }

    @Test
    fun `after a connect from the dialog a clear always runs`() {
        assertTrue(dtcRunsAfterConnect(isRead = false, readsOnConnect = true))
        assertTrue(dtcRunsAfterConnect(isRead = false, readsOnConnect = false))
    }
}
