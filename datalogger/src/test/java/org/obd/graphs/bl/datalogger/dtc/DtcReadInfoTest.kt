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
import org.obd.metrics.command.dtc.DiagnosticTroubleCodeClearStatus

private const val VIN = "ZARFAMAN0K7612345"
private const val OTHER_VIN = "WVWZZZ1KZAW000001"

class DtcReadInfoTest {
    private val labels = DtcReadInfoLabels(lastRead = "Last read: %1\$s", vin = "VIN %1\$s", otherVehicle = "another vehicle (VIN %1\$s)", noVin = "VIN not programmed")

    private fun dtc(code: String) = DiagnosticTroubleCode().apply { standardCode = code }

    @Test
    fun `vin is taken from a metadata key mentioning VIN, padding stripped`() {
        assertEquals(VIN, vinFrom(mapOf("Vehicle identification number (VIN)" to " \u0001${VIN.lowercase()} ")))
        assertEquals(VIN, vinFrom(mapOf("ECU name" to OTHER_VIN, "VIN" to VIN)))
    }

    @Test
    fun `vin is recognised by its value under a label not mentioning VIN`() {
        // giulia_2.0_gme.json labels 22F190 "Vehicle Identification Number"; in Polish it is
        // "Numer identyfikacyjny pojazdu" - neither contains "vin".
        assertEquals(VIN, vinFrom(mapOf("Software number" to "50548170", "Vehicle Identification Number" to VIN)))
        assertEquals(VIN, vinFrom(mapOf("Numer identyfikacyjny pojazdu" to " $VIN ")))
    }

    // mode01.json 09 02 and giulia_2.0_gme.json 22 F190 are both "Vehicle Identification Number": no key mentions "vin",
    // and a VIN-shaped value of another metadata PID was taken for the VIN.
    @Test
    fun `the VIN PIDs' entry wins over a VIN-shaped value of another PID`() {
        val label = "Vehicle Identification Number"
        assertEquals(VIN, vinFrom(mapOf("Calibration ID" to "ABCDEFGH1JKLMN234", label to " $VIN "), setOf(label)))
        assertEquals(VIN, vinFrom(mapOf("ECU VIN code" to OTHER_VIN, label to VIN), setOf(label)))
    }

    @Test
    fun `no vin without a VIN key or a valid 17 character value`() {
        assertNull(vinFrom(emptyMap()))
        assertNull(vinFrom(mapOf("ECU serial number" to "12345678901234567", "Hardware version" to "ABCDEFGHJKLMNPRST")))
        assertNull(vinFrom(mapOf("ECU name" to "$VIN-01")))
        assertNull(vinFrom(mapOf("VIN" to "NO DATA")))
        // I, O and Q never appear in a VIN.
        assertNull(vinFrom(mapOf("VIN" to "ZARFAMAN0K761234O")))
    }

    @Test
    fun `empty VIN reply from a VIN PID gives NO_VIN`() {
        // Unprogrammed 2.0 GME ECM: 22 F190 answers 17 spaces, decoded to "".
        val label = "Numer identyfikacyjny pojazdu"
        assertEquals(NO_VIN, vinFrom(mapOf(label to ""), setOf(label)))
        assertEquals(NO_VIN, vinFrom(mapOf(label to "   "), setOf(label)))
        assertEquals(NO_VIN, vinFrom(mapOf("VIN" to NO_VIN), setOf("VIN")))
    }

    @Test
    fun `NO_VIN only when the VIN PID answered`() {
        // No reply at all, or an empty value under a label that is not a VIN PID: unknown, not empty.
        assertNull(vinFrom(emptyMap(), setOf("VIN")))
        assertNull(vinFrom(mapOf("Hardware version" to ""), setOf("VIN")))
        assertNull(vinFrom(mapOf("VIN" to "")))
        // A real VIN anywhere wins over an empty VIN PID.
        assertEquals(VIN, vinFrom(mapOf("VIN" to "", "Vehicle Identification Number" to VIN), setOf("VIN")))
    }

    @Test
    fun `NO_VIN never names a vehicle`() {
        assertFalse(DtcReadInfo(1, NO_VIN).isFromOtherVehicle(OTHER_VIN))
        assertFalse(DtcReadInfo(1, VIN).isFromOtherVehicle(NO_VIN))
        assertEquals("Last read: 12:00 · VIN not programmed", DtcReadInfo(1, NO_VIN).summary("12:00", VIN, labels))
    }

    @Test
    fun `other vehicle only when both VINs are known and differ`() {
        assertTrue(DtcReadInfo(1, VIN).isFromOtherVehicle(OTHER_VIN))
        assertFalse(DtcReadInfo(1, VIN).isFromOtherVehicle(VIN.lowercase()))
        assertFalse(DtcReadInfo(1, VIN).isFromOtherVehicle(null))
        assertFalse(DtcReadInfo(1, null).isFromOtherVehicle(OTHER_VIN))
    }

    @Test
    fun `summary names the vehicle and flags another one`() {
        assertEquals("Last read: 12:00", DtcReadInfo(1, null).summary("12:00", VIN, labels))
        assertEquals("Last read: 12:00 · VIN $VIN", DtcReadInfo(1, VIN).summary("12:00", VIN, labels))
        assertEquals("Last read: 12:00 · VIN $VIN", DtcReadInfo(1, VIN).summary("12:00", null, labels))
        assertEquals(
            "Last read: 12:00 · another vehicle (VIN $VIN)",
            DtcReadInfo(1, VIN).summary("12:00", OTHER_VIN, labels)
        )
    }

    @Test
    fun `a refused clear is reported even when codes are left`() {
        assertEquals(
            DtcClearOutcome.REFUSED,
            dtcClearOutcome(DiagnosticTroubleCodeClearStatus.ERR, listOf(dtc("P0300")))
        )
        assertEquals(DtcClearOutcome.REFUSED, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.ERR, emptyList()))
    }

    @Test
    fun `codes read back after an acknowledged clear are reported as remaining`() {
        assertEquals(
            DtcClearOutcome.CODES_REMAIN,
            dtcClearOutcome(DiagnosticTroubleCodeClearStatus.OK, listOf(dtc("P0300")))
        )
        // ObdMetrics up to 11.26 reports NO_DATA for every user-triggered clear: the read decides.
        assertEquals(
            DtcClearOutcome.CODES_REMAIN,
            dtcClearOutcome(DiagnosticTroubleCodeClearStatus.NO_DATA, listOf(dtc("P0300")))
        )
    }

    // No tool clears a permanent code (OBD-II 0A): left alone after a clear, it is no sign of a failed clear.
    @Test
    fun `only permanent codes read back after a clear are their own outcome`() {
        val permanent = dtc("P0420").apply { isPermanent = true }
        val storedAndPermanent = dtc("P0300").apply {
            isPermanent = true
            statusMask = 0x08
        }
        assertEquals(DtcClearOutcome.PERMANENT_REMAIN, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.OK, listOf(permanent)))
        assertEquals(DtcClearOutcome.PERMANENT_REMAIN, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.NO_DATA, listOf(permanent)))
        assertEquals(
            DtcClearOutcome.CODES_REMAIN,
            dtcClearOutcome(DiagnosticTroubleCodeClearStatus.OK, listOf(permanent, storedAndPermanent))
        )
        assertEquals(DtcClearOutcome.REFUSED, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.ERR, listOf(permanent)))

        val codes = listOf(permanent, storedAndPermanent, dtc("P0100").apply { statusMask = 0x09 }, dtc(""))
        assertEquals(1, codes.remainingAfterClear(permanent = true))
        assertEquals(2, codes.remainingAfterClear(permanent = false))
    }

    @Test
    fun `nothing read back is a successful clear, a codeless placeholder does not count`() {
        assertEquals(DtcClearOutcome.CLEARED, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.OK, emptyList()))
        assertEquals(DtcClearOutcome.CLEARED, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.NO_DATA, listOf(dtc(""))))
        assertEquals(DtcClearOutcome.CLEARED, dtcClearOutcome(null, emptyList()))
    }

    @Test
    fun `the VIN reply of a DTC action gives the VIN, NO_VIN when empty, null without a reply`() {
        assertEquals("ZAREAEBN9K7617289", vinFromReply("ZAREAEBN9K7617289"))
        assertEquals("ZAREAEBN9K7617289", vinFromReply(" zareaebn9k7617289 "))
        assertEquals(NO_VIN, vinFromReply(""))
        assertEquals(NO_VIN, vinFromReply("00000000000000000"))
        assertNull(vinFromReply(null))
        assertNull(vinFromReply("not a vin"))
    }
}
