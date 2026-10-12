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
    private val labels = DtcReadInfoLabels(lastRead = "Last read: %1\$s", vin = "VIN %1\$s", otherVehicle = "another vehicle (VIN %1\$s)")

    private fun dtc(code: String) = DiagnosticTroubleCode().apply { standardCode = code }

    @Test
    fun `vin is taken from a metadata key mentioning VIN, padding stripped`() {
        assertEquals(VIN, vinFrom(mapOf("Vehicle identification number (VIN)" to " \u0001${VIN.lowercase()} ")))
        assertEquals(VIN, vinFrom(mapOf("ECU name" to OTHER_VIN, "VIN" to VIN)))
    }

    @Test
    fun `no vin without a VIN key or a valid 17 character value`() {
        assertNull(vinFrom(emptyMap()))
        assertNull(vinFrom(mapOf("ECU name" to VIN)))
        assertNull(vinFrom(mapOf("VIN" to "NO DATA")))
        // I, O and Q never appear in a VIN.
        assertNull(vinFrom(mapOf("VIN" to "ZARFAMAN0K761234O")))
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

    @Test
    fun `nothing read back is a successful clear, a codeless placeholder does not count`() {
        assertEquals(DtcClearOutcome.CLEARED, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.OK, emptyList()))
        assertEquals(DtcClearOutcome.CLEARED, dtcClearOutcome(DiagnosticTroubleCodeClearStatus.NO_DATA, listOf(dtc(""))))
        assertEquals(DtcClearOutcome.CLEARED, dtcClearOutcome(null, emptyList()))
    }
}
