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
package org.obd.graphs.bl.query

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.PREF_DYNAMIC_SELECTOR_ENABLED
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.Adapter
import org.obd.graphs.bl.datalogger.DataLoggerSettings
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DragRacingQueryStrategyTest : TestSetup() {

    private lateinit var strategy: DragRacingQueryStrategy

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
        every { sharedPrefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false) } returns false
        strategy = DragRacingQueryStrategy()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getPIDs returns basic speed and rpm pids when GME extensions are disabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(gmeExtensionsEnabled = false)

        assertEquals(setOf(Pid.VEHICLE_SPEED_PID_ID.id, Pid.ENGINE_SPEED_PID_ID.id), strategy.getPIDs())
    }

    @Test
    fun `getPIDs returns extended pids when GME extensions are enabled without STN`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(gmeExtensionsEnabled = true, adapter = Adapter(stnExtensionsEnabled = false))

        val expected = setOf(
            Pid.EXT_VEHICLE_SPEED_PID_ID.id,
            Pid.EXT_ENGINE_SPEED_PID_ID.id,
            Pid.INTAKE_PRESSURE_PID_ID.id,
            Pid.ATM_PRESSURE_PID_ID.id,
            Pid.AMBIENT_TEMP_PID_ID.id
        )
        assertEquals(expected, strategy.getPIDs())
    }

    @Test
    fun `getPIDs adds torque and gas pids when STN extensions are enabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(gmeExtensionsEnabled = true, adapter = Adapter(stnExtensionsEnabled = true))

        val result = strategy.getPIDs()

        assertEquals(7, result.size)
        assertTrue(result.containsAll(setOf(Pid.ENGINE_TORQUE_PID_ID.id, Pid.GAS_PID_ID.id)))
    }

    @Test
    fun `getPIDs adds the dynamic selector pid when its preference is enabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(gmeExtensionsEnabled = true, adapter = Adapter(stnExtensionsEnabled = false))
        every { sharedPrefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false) } returns true

        assertTrue(strategy.getPIDs().contains(Pid.DYNAMIC_SELECTOR_PID_ID.id))
    }
}
