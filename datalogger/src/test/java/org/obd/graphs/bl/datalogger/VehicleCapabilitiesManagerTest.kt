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
package org.obd.graphs.bl.datalogger

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.metrics.api.model.DiagnosticTroubleCode
import org.obd.metrics.api.model.VehicleCapabilities
import org.obd.metrics.command.dtc.DiagnosticTroubleCodeClearStatus
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.PidDefinitionRegistry
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PREF_VEHICLE_SUPPORTED_PIDS = "pref.datalogger.supported.pids"
private const val PREF_VEHICLE_METADATA = "pref.datalogger.vehicle.properties"
private const val PREF_DTC = "pref.datalogger.dtc"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class VehicleCapabilitiesManagerTest : TestSetup() {

    private fun dtc(
        standardCode: String,
        description: String
    ) = DiagnosticTroubleCode().apply {
        this.standardCode = standardCode
        this.description = description
    }

    private fun vehicleCapabilities(
        metadata: Map<String, String> = emptyMap(),
        capabilities: Set<String> = emptySet(),
        dtc: Set<DiagnosticTroubleCode> = emptySet()
    ) = VehicleCapabilities(metadata, capabilities, dtc, DiagnosticTroubleCodeClearStatus.OK)

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
        mockkStatic("org.obd.graphs.BroadcastKt")
        every { org.obd.graphs.sendBroadcastEvent(any<String>()) } returns Unit
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `updateCapabilities stores supported pids when reading is enabled and capabilities are present`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(vehicleCapabilitiesReadingEnabled = true, dtcEnabled = false))

        val capabilities = vehicleCapabilities(metadata = mapOf("vin" to "WVW123"), capabilities = setOf("0100", "010C"))

        VehicleCapabilitiesManager.updateCapabilities(capabilities)

        verify { editor.putStringSet(PREF_VEHICLE_SUPPORTED_PIDS, capabilities.capabilities) }
        verify { editor.putString(eq(PREF_VEHICLE_METADATA), any()) }
    }

    @Test
    fun `updateCapabilities does not store supported pids when reading is disabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(vehicleCapabilitiesReadingEnabled = false, dtcEnabled = false))

        val capabilities = vehicleCapabilities(capabilities = setOf("0100"))

        VehicleCapabilitiesManager.updateCapabilities(capabilities)

        verify(exactly = 0) { editor.putStringSet(eq(PREF_VEHICLE_SUPPORTED_PIDS), any()) }
    }

    @Test
    fun `updateCapabilities broadcasts DTC available only when dtc reading is enabled and dtc is not empty`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(vehicleCapabilitiesReadingEnabled = false, dtcEnabled = true))

        val capabilities = vehicleCapabilities(dtc = setOf(dtc("P0100", "Mass air flow circuit")))

        VehicleCapabilitiesManager.updateCapabilities(capabilities)

        verify { org.obd.graphs.sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE) }
    }

    @Test
    fun `updateCapabilities does not broadcast when dtc reading is disabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(vehicleCapabilitiesReadingEnabled = false, dtcEnabled = false))

        val capabilities = vehicleCapabilities(dtc = setOf(dtc("P0100", "Mass air flow circuit")))

        VehicleCapabilitiesManager.updateCapabilities(capabilities)

        verify(exactly = 0) { org.obd.graphs.sendBroadcastEvent(DATA_LOGGER_DTC_AVAILABLE) }
    }

    @Test
    fun `updateDTC and getDiagnosticTroubleCodes round trip through preferences`() {
        val slot = slot<String>()
        every { editor.putString(eq(PREF_DTC), capture(slot)) } returns editor

        VehicleCapabilitiesManager.updateDTC(setOf(dtc("P0100", "Mass air flow circuit")))

        every { sharedPrefs.getString(eq(PREF_DTC), any()) } returns slot.captured

        val result = VehicleCapabilitiesManager.getDiagnosticTroubleCodes()

        assertEquals(1, result.size)
        assertEquals("P0100", result[0].standardCode)
    }

    @Test
    fun `getDiagnosticTroubleCodes returns an empty list instead of throwing on malformed json`() {
        every { sharedPrefs.getString(eq(PREF_DTC), any()) } returns "not-json"

        assertTrue(VehicleCapabilitiesManager.getDiagnosticTroubleCodes().isEmpty())
    }

    @Test
    fun `getVehicleMetadata returns an empty list when nothing is stored`() {
        assertTrue(VehicleCapabilitiesManager.getVehicleMetadata().isEmpty())
    }

    @Test
    fun `getVehicleMetadata parses the stored metadata map`() {
        every { sharedPrefs.getString(eq(PREF_VEHICLE_METADATA), any()) } returns """{"vin":"WVW123"}"""

        val result = VehicleCapabilitiesManager.getVehicleMetadata()

        assertEquals(1, result.size)
        assertEquals("vin", result[0].name)
        assertEquals("WVW123", result[0].value)
    }

    @Test
    fun `getSupportedPIDs returns the stored pids`() {
        mockkObject(DataLoggerRepository)
        val mockRegistry = mockk<PidDefinitionRegistry>()
        val pid1 = PidDefinition(1L, "0100", "atsh6f1", "Supported PIDs", "01", "SomeCodec")
        val pid2 = PidDefinition(2L, "010C", "atsh6f1", "Engine RPM", "01", "SomeCodec")
        every { mockRegistry.findAll() } returns listOf(pid1, pid2)
        every { DataLoggerRepository.getPidDefinitionRegistry() } returns mockRegistry

        every { sharedPrefs.getStringSet(eq(PREF_VEHICLE_SUPPORTED_PIDS), any()) } returns setOf("0100", "010c")

        val result = VehicleCapabilitiesManager.getSupportedPIDs()

        assertEquals(setOf("0100", "010c"), result.toSet())
    }
}
