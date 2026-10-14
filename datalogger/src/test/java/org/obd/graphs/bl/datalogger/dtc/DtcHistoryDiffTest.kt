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
import org.obd.metrics.api.model.DiagnosticTroubleCode

class DtcHistoryDiffTest {
    private fun dtc(
        code: String,
        module: String? = null,
        statusMask: Int = 0x08
    ) = DiagnosticTroubleCode().apply {
        standardCode = code
        description = "desc $code"
        this.module = module
        this.statusMask = statusMask
    }

    private fun scan(
        timestamp: Long,
        vararg codes: DiagnosticTroubleCode,
        modules: List<String> = emptyList(),
        kind: DtcScanKind = DtcScanKind.READ
    ) = DtcScan(timestamp, kind, modules, codes.toList())

    @Test
    fun `the first scan of a vehicle marks nothing new and nothing gone`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100"))))

        assertTrue(diff.newCodes.isEmpty())
        assertTrue(diff.gone.isEmpty())
        assertEquals(DtcCodeHistory(1, 1, 1, 1), diff.history(dtc("P0100")))
    }

    @Test
    fun `a code no earlier scan reported is new`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100")), scan(2, dtc("P0100"), dtc("P0200"))))

        assertTrue(diff.isNew(dtc("P0200")))
        assertFalse(diff.isNew(dtc("P0100")))
    }

    @Test
    fun `an intermittent code is not new when it comes back`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100")), scan(2), scan(3, dtc("P0100"))))

        assertFalse(diff.isNew(dtc("P0100")))
        assertEquals(DtcCodeHistory(firstSeen = 1, lastSeen = 3, seenIn = 2, coveredIn = 3), diff.history(dtc("P0100")))
    }

    @Test
    fun `a code the previous scan had and this one has not is gone`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100"), dtc("P0200")), scan(2, dtc("P0100"))))

        assertEquals(listOf("P0200"), diff.gone.map { it.standardCode })
        assertEquals(DtcCodeHistory(1, 1, 1, 2), diff.history(dtc("P0200")))
    }

    @Test
    fun `a code gone before the previous scan is not listed again`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0200")), scan(2), scan(3)))

        assertTrue(diff.gone.isEmpty())
    }

    @Test
    fun `codes of a module left out of the scan are not gone`() {
        val engine = dtc("P0100", module = "Engine")
        val abs = dtc("C0035", module = "ABS")
        val diff =
            dtcHistoryDiff(
                listOf(
                    scan(1, engine, abs, modules = listOf("Engine", "ABS")),
                    scan(2, modules = listOf("Engine"))
                )
            )

        assertEquals(listOf("P0100"), diff.gone.map { it.standardCode })
    }

    @Test
    fun `a code from a module never scanned before is not new`() {
        val diff =
            dtcHistoryDiff(
                listOf(
                    scan(1, modules = listOf("Engine")),
                    scan(2, dtc("C0035", module = "ABS"), modules = listOf("Engine", "ABS"))
                )
            )

        assertFalse(diff.isNew(dtc("C0035", module = "ABS")))
    }

    @Test
    fun `the same code from two modules is two codes`() {
        val diff =
            dtcHistoryDiff(
                listOf(
                    scan(1, dtc("U0100", module = "ABS"), modules = listOf("ABS", "BCM")),
                    scan(2, dtc("U0100", module = "ABS"), dtc("U0100", module = "BCM"), modules = listOf("ABS", "BCM"))
                )
            )

        assertTrue(diff.isNew(dtc("U0100", module = "BCM")))
        assertFalse(diff.isNew(dtc("U0100", module = "ABS")))
    }

    @Test
    fun `a default read is compared only with other default reads`() {
        // The module scan on connect reports the same fault tagged with its module: the default-ECU code of
        // the first read is neither gone after it, nor does the next default read find it new.
        val diff =
            dtcHistoryDiff(
                listOf(
                    scan(1, dtc("P0100", module = DTC_DEFAULT_MODULE)),
                    scan(2, dtc("P0100", module = "Engine"), modules = listOf("Engine")),
                    scan(3, dtc("P0100", module = DTC_DEFAULT_MODULE), dtc("P0300"))
                )
            )

        assertFalse(diff.isNew(dtc("P0100")))
        assertTrue(diff.isNew(dtc("P0300")))
        assertTrue(diff.gone.isEmpty())
        assertEquals(DtcCodeHistory(1, 3, 2, 2), diff.history(dtc("P0100")))
    }

    @Test
    fun `a status change keeps the code the same code`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100", statusMask = 0x04)), scan(2, dtc("P0100", statusMask = 0x09))))

        assertTrue(diff.newCodes.isEmpty())
        assertTrue(diff.gone.isEmpty())
    }

    @Test
    fun `no scans give no markers`() {
        assertEquals(DtcHistoryDiff.EMPTY, dtcHistoryDiff(emptyList()))
        assertNull(DtcHistoryDiff.EMPTY.history(dtc("P0100")))
    }

    @Test
    fun `list rows are marked and gone codes get a collapsed section`() {
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100"), dtc("P0200")), scan(2, dtc("P0100"), dtc("P0300"))))
        val items =
            listOf(dtc("P0100"), dtc("P0300"))
                .toDtcListItems(emptyList(), "none", "none for module", "unknown")
                .markedWith(diff)

        val rows = items.filterIsInstance<DtcListItem.DtcRow>()
        assertEquals(listOf(false, true), rows.map { it.isNew })
        assertEquals(DtcCodeHistory(1, 2, 2, 2), rows[0].history)

        val collapsed = items.withGoneSection(diff, expanded = false)
        assertEquals(DtcListItem.GoneHeader(1, expanded = false), collapsed.last())

        val expanded = items.withGoneSection(diff, expanded = true)
        val gone = expanded.last() as DtcListItem.DtcRow
        assertEquals(DtcListItem.GoneHeader(1, expanded = true), expanded[expanded.size - 2])
        assertTrue(gone.isGone)
        assertEquals("P0200", gone.dtc.standardCode)
        // Marking never changes a row's identity.
        assertEquals(DtcListItem.DtcRow(dtc("P0300")).itemKey(), rows[1].itemKey())
    }

    @Test
    fun `no gone section without gone codes`() {
        val items = listOf(DtcListItem.DtcRow(dtc("P0100")))

        assertEquals(items, items.withGoneSection(DtcHistoryDiff.EMPTY, expanded = true))
    }
}
