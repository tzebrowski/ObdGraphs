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

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.SnapshotPID
import org.obd.metrics.pid.PidDefinition
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DtcHistoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var file: File

    private fun dtc(
        code: String,
        statusMask: Int = 0x08
    ) = DiagnosticTroubleCode().apply {
        standardCode = code
        description = "desc $code"
        this.statusMask = statusMask
    }

    private fun scan(
        timestamp: Long,
        vararg codes: DiagnosticTroubleCode,
        kind: DtcScanKind = DtcScanKind.READ
    ) = DtcScan(timestamp, kind, emptyList(), codes.toList())

    @Before
    fun setup() {
        file = File(folder.root, "dtc_history.json")
        DtcHistoryStore.file = { file }
        DtcHistoryStore.reset()
    }

    @After
    fun tearDown() = DtcHistoryStore.reset()

    @Test
    fun `scans are kept per vehicle, oldest dropped past the cap`() {
        val history =
            (1..DTC_HISTORY_MAX_SCANS + 5).fold(emptyMap<String, List<DtcScan>>()) { acc, i ->
                acc.append("ZARFAMAN0K7612345", scan(i.toLong(), dtc("P0100")))
            }

        val scans = history.getValue("ZARFAMAN0K7612345")
        assertEquals(DTC_HISTORY_MAX_SCANS, scans.size)
        assertEquals(6L, scans.first().timestamp)
        assertEquals(DTC_HISTORY_MAX_SCANS + 5L, scans.last().timestamp)
    }

    @Test
    fun `the least recently scanned vehicle is dropped past the vehicle cap`() {
        val history =
            (1..DTC_HISTORY_MAX_VEHICLES + 1).fold(emptyMap<String, List<DtcScan>>()) { acc, i ->
                acc.append("VIN$i", scan(i.toLong()))
            }

        assertEquals(DTC_HISTORY_MAX_VEHICLES, history.size)
        assertTrue("VIN1" !in history)
        assertTrue("VIN${DTC_HISTORY_MAX_VEHICLES + 1}" in history)
    }

    @Test
    fun `a connect scan equal to the previous connect scan replaces it`() {
        val history =
            emptyMap<String, List<DtcScan>>()
                .append("VIN", scan(1, dtc("P0100"), kind = DtcScanKind.CONNECT))
                .append("VIN", scan(2, dtc("P0100"), kind = DtcScanKind.CONNECT))
                .append("VIN", scan(3, dtc("P0100"), kind = DtcScanKind.CONNECT))

        assertEquals(listOf(3L), history.getValue("VIN").map { it.timestamp })
    }

    @Test
    fun `a connect scan is appended when the codes, their state, or the previous kind differ`() {
        val history =
            emptyMap<String, List<DtcScan>>()
                .append("VIN", scan(1, dtc("P0100"), kind = DtcScanKind.CONNECT))
                .append("VIN", scan(2, dtc("P0100", statusMask = 0x09), kind = DtcScanKind.CONNECT))
                .append("VIN", scan(3, dtc("P0100", statusMask = 0x09)))
                .append("VIN", scan(4, dtc("P0100", statusMask = 0x09), kind = DtcScanKind.CONNECT))

        assertEquals(listOf(1L, 2L, 3L, 4L), history.getValue("VIN").map { it.timestamp })
    }

    @Test
    fun `reads without a VIN go under the unknown vehicle`() {
        val history = emptyMap<String, List<DtcScan>>().append(null, scan(1))

        assertEquals(setOf(UNKNOWN_VEHICLE), history.keys)
    }

    @Test
    fun `the store survives a restart, freeze frames included`() {
        val counter = PidDefinition(1239005L, 1, "A", "19", "6080", "", "DTC Event Counter", 0, 255, null)
        val code = dtc("P0100").apply { snapshot = listOf(SnapshotPID(counter, "07", 7)) }
        DtcHistoryStore.append("VIN", scan(1, code))
        DtcHistoryStore.append("VIN", scan(2, kind = DtcScanKind.CLEAR))

        DtcHistoryStore.reset()
        val scans = DtcHistoryStore.scans("VIN")

        assertEquals(listOf(DtcScanKind.READ, DtcScanKind.CLEAR), scans.map { it.kind })
        assertEquals(7, scans[0].codes.single().snapshot.single().decodedValue.toInt())
    }

    @Test
    fun `a corrupt file is an empty history and is replaced by the next scan`() {
        file.writeText("{not json")

        assertTrue(DtcHistoryStore.scans("VIN").isEmpty())

        DtcHistoryStore.append("VIN", scan(1))
        DtcHistoryStore.reset()
        assertEquals(1, DtcHistoryStore.scans("VIN").size)
    }

    @Test
    fun `diff marks only the list that is the vehicle's newest scan`() {
        DtcHistoryStore.append("VIN", scan(1, dtc("P0100")))
        DtcHistoryStore.append("VIN", scan(2, dtc("P0100"), dtc("P0200")))

        assertEquals(setOf(DtcListItem.DtcRow(dtc("P0200")).itemKey()), DtcHistoryStore.diff(DtcReadInfo(2, "VIN")).newCodes)
        // A list stored before history existed, or whose history write failed.
        assertEquals(DtcHistoryDiff.EMPTY, DtcHistoryStore.diff(DtcReadInfo(1, "VIN")))
        assertEquals(DtcHistoryDiff.EMPTY, DtcHistoryStore.diff(null))
    }

    @Test
    fun `the unknown vehicle gets no markers, an unprogrammed VIN does`() {
        DtcHistoryStore.append(null, scan(1))
        DtcHistoryStore.append(null, scan(2, dtc("P0200")))
        DtcHistoryStore.append(NO_VIN, scan(1))
        DtcHistoryStore.append(NO_VIN, scan(2, dtc("P0200")))

        assertEquals(DtcHistoryDiff.EMPTY, DtcHistoryStore.diff(DtcReadInfo(2, null)))
        assertEquals(1, DtcHistoryStore.diff(DtcReadInfo(2, NO_VIN)).newCodes.size)
    }

    @Test
    fun `an unwritable location never throws`() {
        DtcHistoryStore.file = { File(folder.root, "missing/dir/dtc_history.json") }

        DtcHistoryStore.append("VIN", scan(1))
    }
}
