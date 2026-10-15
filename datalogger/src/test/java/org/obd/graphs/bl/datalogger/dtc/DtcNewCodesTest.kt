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

    private val vin = "ZARFAMAN0K7612345"

    private fun announced(
        current: List<DiagnosticTroubleCode>,
        vararg earlier: DtcScan,
        kind: DtcScanKind = DtcScanKind.CONNECT,
        vin: String? = this.vin
    ) = dtcAnnouncement(kind, vin, current, earlier.toList() + scan(99, *current.toTypedArray(), kind = kind), firstScan = earlier.isEmpty())

    @Test
    fun `a connect scan announces only the codes new to the vehicle`() {
        val announcement = announced(listOf(dtc("P0100"), dtc("P0200")), scan(1, dtc("P0100")))!!

        assertEquals(listOf("P0200"), announcement.lines)
        assertEquals(listOf("P0200"), announcement.codes)
        assertFalse(announcement.firstScan)
    }

    @Test
    fun `a read or a clear started by the user announces nothing`() {
        val current = listOf(dtc("P0100"), dtc("P0200"))

        assertNull(announced(current, scan(1, dtc("P0100")), kind = DtcScanKind.READ))
        assertNull(announced(current, scan(1, dtc("P0100")), kind = DtcScanKind.CLEAR))
    }

    @Test
    fun `the first scan of a vehicle announces every code it found`() {
        val announcement = announced(listOf(dtc("P0200"), dtc("P0100")))!!

        assertEquals(listOf("P0100", "P0200"), announcement.lines)
        assertTrue(announcement.firstScan)
    }

    @Test
    fun `a first scan without codes announces nothing`() {
        assertNull(announced(emptyList()))
    }

    @Test
    fun `a vehicle without a VIN reply announces nothing, one without a programmed VIN does`() {
        assertNull(announced(listOf(dtc("P0100")), vin = null))
        assertTrue(announced(listOf(dtc("P0100")), vin = NO_VIN)!!.firstScan)
    }

    @Test
    fun `codes already reported before are not announced again`() {
        assertNull(announced(listOf(dtc("P0100")), scan(1, dtc("P0100")), scan(2)))
    }

    @Test
    fun `announced codes are in display order`() {
        val current = listOf(dtc("U0100", "Body"), dtc("P0300", "Engine"), dtc("P0200", "Engine"))

        assertEquals(
            listOf("U0100 · Body", "P0200 · Engine", "P0300 · Engine"),
            announced(current)!!.lines
        )
    }

    @Test
    fun `an announcement line names the module, the default module stays unnamed`() {
        assertEquals("P0100 · Engine ECU", dtc("P0100", "Engine ECU").announcementLine())
        assertEquals("P0100", dtc("P0100").announcementLine())
    }

    @Test
    fun `a long announcement shows its first lines and counts the rest`() {
        val announcement = DtcAnnouncement((1..10).map { "P0$it" }, firstScan = true)

        assertEquals(listOf("P01", "P02", "P03") to 7, announcement.shown(3))
        assertEquals(announcement.lines to 0, announcement.shown(20))
    }

    @Test
    fun `the pop-up line lists the first codes alone and counts the rest`() {
        val announcement = announced((1..5).map { dtc("P010$it", "Engine ECU") })!!

        assertEquals("P0101, P0102, P0103 +2", announcement.summary(3))
        assertEquals("P0101, P0102, P0103, P0104, P0105", announcement.summary(5))
    }
}
