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
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.USER_CUSTOM_PIDS_FILE
import org.obd.graphs.bl.TestSetup
import org.obd.metrics.api.model.ObdMetric
import org.obd.metrics.command.obd.ObdCommand
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ObdMetricExtTest : TestSetup() {

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
    }

    @After
    fun tearDown() = unmockkAll()

    private fun pidWithId(id: Long): PidDefinition = PidDefinition(id, "010C", "atsh6f1", "Test PID", "01", "SomeCodec")

    private fun pidWithRange(
        id: Long,
        min: Number,
        max: Number
    ): PidDefinition = PidDefinition(id, 1, "", "01", "0100", "%", "Test PID", min, max, ValueType.DOUBLE)

    private fun metricFor(
        pid: PidDefinition,
        value: Any?
    ): ObdMetric = ObdMetric.builder().command(ObdCommand(pid)).value(value).build()

    @Test
    fun `isAtmPressure matches only the atm pressure pid id`() {
        assertTrue(metricFor(pidWithId(Pid.ATM_PRESSURE_PID_ID.id), 100).isAtmPressure())
        assertFalse(metricFor(pidWithId(Pid.AMBIENT_TEMP_PID_ID.id), 100).isAtmPressure())
    }

    @Test
    fun `isAmbientTemp matches only the ambient temp pid id`() {
        assertTrue(metricFor(pidWithId(Pid.AMBIENT_TEMP_PID_ID.id), 20).isAmbientTemp())
        assertFalse(metricFor(pidWithId(Pid.ATM_PRESSURE_PID_ID.id), 20).isAmbientTemp())
    }

    @Test
    fun `isVehicleStatus matches only the vehicle status pid id`() {
        assertTrue(metricFor(pidWithId(Pid.VEHICLE_STATUS_PID_ID.id), null).isVehicleStatus())
        assertFalse(metricFor(pidWithId(Pid.ATM_PRESSURE_PID_ID.id), null).isVehicleStatus())
    }

    @Test
    fun `isDynamicSelector matches only the dynamic selector pid id`() {
        assertTrue(metricFor(pidWithId(Pid.DYNAMIC_SELECTOR_PID_ID.id), 1).isDynamicSelector())
        assertFalse(metricFor(pidWithId(Pid.ATM_PRESSURE_PID_ID.id), 1).isDynamicSelector())
    }

    @Test
    fun `isVehicleSpeed uses standard pid when gme extensions are disabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(gmeExtensionsEnabled = false)

        assertTrue(metricFor(pidWithId(Pid.VEHICLE_SPEED_PID_ID.id), 50).isVehicleSpeed())
        assertFalse(metricFor(pidWithId(Pid.EXT_VEHICLE_SPEED_PID_ID.id), 50).isVehicleSpeed())
    }

    @Test
    fun `isVehicleSpeed uses extended pid when gme extensions are enabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(gmeExtensionsEnabled = true)

        assertTrue(metricFor(pidWithId(Pid.EXT_VEHICLE_SPEED_PID_ID.id), 50).isVehicleSpeed())
        assertFalse(metricFor(pidWithId(Pid.VEHICLE_SPEED_PID_ID.id), 50).isVehicleSpeed())
    }

    @Test
    fun `isEngineRpm uses standard pid when gme extensions are disabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(gmeExtensionsEnabled = false)

        assertTrue(metricFor(pidWithId(Pid.ENGINE_SPEED_PID_ID.id), 3000).isEngineRpm())
        assertFalse(metricFor(pidWithId(Pid.EXT_ENGINE_SPEED_PID_ID.id), 3000).isEngineRpm())
    }

    @Test
    fun `isEngineRpm uses extended pid when gme extensions are enabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(gmeExtensionsEnabled = true)

        assertTrue(metricFor(pidWithId(Pid.EXT_ENGINE_SPEED_PID_ID.id), 3000).isEngineRpm())
        assertFalse(metricFor(pidWithId(Pid.ENGINE_SPEED_PID_ID.id), 3000).isEngineRpm())
    }

    @Test
    fun `isUserCustom is true only for the user custom pids resource file`() {
        val pid = pidWithId(99L)

        pid.resourceFile = USER_CUSTOM_PIDS_FILE
        assertTrue(pid.isUserCustom)

        pid.resourceFile = "alfa.json"
        assertFalse(pid.isUserCustom)
    }

    @Test
    fun `PidDefinition scaleToRange maps a 0-3500 value into the pid's own range`() {
        val pid = pidWithRange(id = 1L, min = 0, max = 100)

        // 1750 is the midpoint of the default 0..3500 range, should map to the midpoint of 0..100
        val result = pid.scaleToRange(1750f)

        assertEquals(50f, result, 0.01f)
    }

    @Test
    fun `PidDefinition scaleToRange falls back to 0-9999 range when pid has no min or max`() {
        // Regression test: min/max are a platform-typed java.lang.Number in the underlying
        // library, so a custom PID saved without them previously NPE'd here during trip upload.
        val pid = PidDefinition(1L, "010C", "atsh6f1", "Test PID", "01", "SomeCodec")

        val result = pid.scaleToRange(1750f)

        // 1750.mapRange(0, 3500, 0, 9999)
        val expected = 1750f.let { (it - 0f) * (9999f - 0f) / (3500f - 0f) }
        assertEquals(expected, result, 0.01f)
    }

    @Test
    fun `ObdMetric scaleToRange maps the metric value into the 0-3500 range`() {
        val pid = pidWithRange(id = 1L, min = 0, max = 100)
        val metric = metricFor(pid, 50)

        val result = metric.scaleToRange()

        assertEquals(1750f, result, 0.01f)
    }

    @Test
    fun `ObdMetric scaleToRange falls back to 0-9999 range when pid has no min or max`() {
        val pid = PidDefinition(1L, "010C", "atsh6f1", "Test PID", "01", "SomeCodec")
        val metric = metricFor(pid, 100)

        val result = metric.scaleToRange()

        // 100.mapRange(0, 9999, 0, 3500)
        val expected = 100f.let { (it - 0f) * (3500f - 0f) / (9999f - 0f) }
        assertEquals(expected, result, 0.01f)
    }
}
