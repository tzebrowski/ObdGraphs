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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.command.dtc.DtcModuleStatus

class DtcStoredListTest {
    private fun dtc(
        code: String,
        module: String
    ) = DiagnosticTroubleCode().apply {
        standardCode = code
        this.module = module
    }

    private val previous =
        DtcStoredList(
            codes = listOf(dtc("P0100", "Engine ECU"), dtc("C0035", "ABS")),
            scannedModules = listOf("Engine ECU", "ABS", "BCM"),
            moduleStatus = mapOf("Engine ECU" to DtcModuleStatus.RESPONDED, "ABS" to DtcModuleStatus.RESPONDED, "BCM" to DtcModuleStatus.NO_RESPONSE)
        )

    private val engineCleared = DtcStoredList(emptyList(), listOf("Engine ECU"), mapOf("Engine ECU" to DtcModuleStatus.RESPONDED))

    @Test
    fun `clearing one module keeps the other modules' codes and statuses`() {
        val merged = engineCleared.mergedOver(previous, DtcScanKind.CLEAR, sameVehicle = true)

        assertEquals(listOf("C0035"), merged.codes.map { it.standardCode })
        assertEquals(setOf("Engine ECU", "ABS", "BCM"), merged.scannedModules.toSet())
        assertEquals(DtcModuleStatus.NO_RESPONSE, merged.moduleStatus["BCM"])
        assertEquals(DtcModuleStatus.RESPONDED, merged.moduleStatus["Engine ECU"])
    }

    @Test
    fun `the targeted module's codes come from the new read only`() {
        val read = DtcStoredList(listOf(dtc("P0200", "Engine ECU")), listOf("Engine ECU"))

        val merged = read.mergedOver(previous, DtcScanKind.READ, sameVehicle = true)

        assertEquals(setOf("P0200", "C0035"), merged.codes.map { it.standardCode }.toSet())
    }

    @Test
    fun `a connect scan, another vehicle or a default read replace the list`() {
        assertEquals(engineCleared, engineCleared.mergedOver(previous, DtcScanKind.CONNECT, sameVehicle = true))
        assertEquals(engineCleared, engineCleared.mergedOver(previous, DtcScanKind.CLEAR, sameVehicle = false))
        assertEquals(engineCleared, engineCleared.mergedOver(null, DtcScanKind.CLEAR, sameVehicle = true))

        val defaultRead = DtcStoredList(listOf(dtc("P0300", "ecu")), emptyList())
        assertEquals(defaultRead, defaultRead.mergedOver(previous, DtcScanKind.READ, sameVehicle = true))
        // A default-ECU list has no modules to keep.
        assertEquals(engineCleared, engineCleared.mergedOver(defaultRead, DtcScanKind.CLEAR, sameVehicle = true))
    }

    @Test
    fun `only reads stamped with the same known VIN are the same vehicle`() {
        val vin = "ZARFAMAN0K7612345"
        assertTrue(isSameVehicle(DtcReadInfo(1, vin.lowercase()), vin))
        assertTrue(isSameVehicle(DtcReadInfo(1, NO_VIN), NO_VIN))
        assertFalse(isSameVehicle(DtcReadInfo(1, vin), "ZARFAMAN0K7699999"))
        assertFalse(isSameVehicle(DtcReadInfo(1, null), null))
        assertFalse(isSameVehicle(null, vin))
    }
}
