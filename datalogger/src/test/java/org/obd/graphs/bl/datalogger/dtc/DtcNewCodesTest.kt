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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.metrics.api.model.DiagnosticTroubleCode

class DtcNewCodesTest {
    private fun dtc(
        code: String,
        module: String? = null
    ) = DiagnosticTroubleCode().apply {
        standardCode = code
        description = "desc $code"
        this.module = module
        statusMask = 0x08
    }

    private fun scan(
        timestamp: Long,
        vararg codes: DiagnosticTroubleCode,
        kind: DtcScanKind = DtcScanKind.CONNECT
    ) = DtcScan(timestamp, kind, emptyList(), codes.toList())

    @Test
    fun `a connect scan announces only the codes new to the vehicle`() {
        val current = listOf(dtc("P0100"), dtc("P0200"))
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100")), scan(2, *current.toTypedArray())))

        assertEquals(listOf("P0200"), dtcCodesToAnnounce(DtcScanKind.CONNECT, current, diff).map { it.standardCode })
    }

    @Test
    fun `a read or a clear started by the user announces nothing`() {
        val current = listOf(dtc("P0100"), dtc("P0200"))
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100")), scan(2, *current.toTypedArray())))

        assertTrue(dtcCodesToAnnounce(DtcScanKind.READ, current, diff).isEmpty())
        assertTrue(dtcCodesToAnnounce(DtcScanKind.CLEAR, current, diff).isEmpty())
    }

    @Test
    fun `the first scan of a vehicle announces nothing`() {
        val current = listOf(dtc("P0100"))

        assertTrue(dtcCodesToAnnounce(DtcScanKind.CONNECT, current, dtcHistoryDiff(listOf(scan(1, *current.toTypedArray())))).isEmpty())
    }

    @Test
    fun `codes already reported before are not announced again`() {
        val current = listOf(dtc("P0100"))
        val diff = dtcHistoryDiff(listOf(scan(1, dtc("P0100")), scan(2), scan(3, *current.toTypedArray())))

        assertTrue(dtcCodesToAnnounce(DtcScanKind.CONNECT, current, diff).isEmpty())
    }

    @Test
    fun `nothing is announced without history markers`() {
        assertTrue(dtcCodesToAnnounce(DtcScanKind.CONNECT, listOf(dtc("P0100")), DtcHistoryDiff.EMPTY).isEmpty())
    }

    @Test
    fun `announced codes are in display order`() {
        val current = listOf(dtc("U0100", "Body"), dtc("P0300", "Engine"), dtc("P0200", "Engine"))
        val diff = dtcHistoryDiff(listOf(scan(1), scan(2, *current.toTypedArray())))
            .copy(newCodes = current.map { it.historyKey() }.toSet())

        assertEquals(
            listOf("U0100", "P0200", "P0300"),
            dtcCodesToAnnounce(DtcScanKind.CONNECT, current, diff).map { it.standardCode }
        )
    }

    @Test
    fun `an announcement line names the module, the default module stays unnamed`() {
        assertEquals("P0100 · Engine ECU", dtc("P0100", "Engine ECU").announcementLine())
        assertEquals("P0100", dtc("P0100").announcementLine())
    }
}
