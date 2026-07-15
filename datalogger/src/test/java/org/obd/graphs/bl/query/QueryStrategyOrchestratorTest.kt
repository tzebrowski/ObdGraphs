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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.Adapter
import org.obd.graphs.bl.datalogger.DataLoggerSettings
import org.obd.graphs.bl.datalogger.Pid
import org.obd.graphs.bl.datalogger.dataLoggerSettings
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QueryStrategyOrchestratorTest : TestSetup() {

    private lateinit var orchestrator: QueryStrategyOrchestrator

    @Before
    override fun setup() {
        super.setup()
        mockkObject(dataLoggerSettings)
        every { dataLoggerSettings.instance() } returns DataLoggerSettings()
        orchestrator = QueryStrategyOrchestrator()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `default strategy is SHARED_QUERY`() {
        assertEquals(QueryStrategyType.SHARED_QUERY, orchestrator.getStrategy())
    }

    @Test
    fun `setStrategy updates the current strategy`() {
        orchestrator.setStrategy(QueryStrategyType.PERFORMANCE_QUERY)
        assertEquals(QueryStrategyType.PERFORMANCE_QUERY, orchestrator.getStrategy())
    }

    @Test
    fun `getDefaultPIDs and getIDs delegate to the selected strategy`() {
        orchestrator.setStrategy(QueryStrategyType.ROUTINES_QUERY)
        orchestrator.update(setOf(42L))

        assertEquals(setOf(42L), orchestrator.getIDs())
        assertEquals(emptySet<Long>(), orchestrator.getDefaultPIDs())
    }

    @Test
    fun `getIDs adds the vehicle status pid when the vehicle status panel is enabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(vehicleStatusPanelEnabled = true)
        orchestrator.setStrategy(QueryStrategyType.ROUTINES_QUERY)

        assertEquals(setOf(Pid.VEHICLE_STATUS_PID_ID.id), orchestrator.getIDs())
    }

    @Test
    fun `getIDs adds the vehicle status pid when disconnect-when-off is enabled`() {
        every { dataLoggerSettings.instance() } returns DataLoggerSettings(vehicleStatusDisconnectWhenOff = true)
        orchestrator.setStrategy(QueryStrategyType.ROUTINES_QUERY)

        assertEquals(setOf(Pid.VEHICLE_STATUS_PID_ID.id), orchestrator.getIDs())
    }

    @Test
    fun `getIDs always strips the GPS location pid`() {
        orchestrator.setStrategy(QueryStrategyType.ROUTINES_QUERY)
        orchestrator.update(setOf(Pid.GPS_LOCATION_PID_ID.id, 1L))

        assertEquals(setOf(1L), orchestrator.getIDs())
    }

    @Test
    fun `filterBy returns full selection when individual query strategy is enabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(individualQueryStrategyEnabled = true))
        every { sharedPrefs.getStringSet("some.filter.key", any()) } returns setOf("5", "6")

        orchestrator.setStrategy(QueryStrategyType.ROUTINES_QUERY)
        orchestrator.update(setOf(1L))

        assertEquals(setOf(5L, 6L), orchestrator.filterBy("some.filter.key"))
    }

    @Test
    fun `filterBy returns the intersection with current query when individual query is disabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(individualQueryStrategyEnabled = false))
        every { sharedPrefs.getStringSet("some.filter.key", any()) } returns setOf("1", "2", "3")

        orchestrator.setStrategy(QueryStrategyType.ROUTINES_QUERY)
        orchestrator.update(setOf(2L, 3L, 4L))

        assertEquals(setOf(2L, 3L), orchestrator.filterBy("some.filter.key"))
    }

    @Test
    fun `apply(String) switches to INDIVIDUAL_QUERY and updates pids when individual query is enabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(individualQueryStrategyEnabled = true))
        every { sharedPrefs.getStringSet("some.filter.key", any()) } returns setOf("7")

        orchestrator.apply("some.filter.key")

        assertEquals(QueryStrategyType.INDIVIDUAL_QUERY, orchestrator.getStrategy())
        assertEquals(setOf(7L), orchestrator.getIDs())
    }

    @Test
    fun `apply(String) forces SHARED_QUERY when individual query is disabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(individualQueryStrategyEnabled = false))

        orchestrator.setStrategy(QueryStrategyType.PERFORMANCE_QUERY)
        orchestrator.apply("some.filter.key")

        assertEquals(QueryStrategyType.SHARED_QUERY, orchestrator.getStrategy())
    }

    @Test
    fun `apply(Set) switches to INDIVIDUAL_QUERY and updates pids when individual query is enabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(individualQueryStrategyEnabled = true))

        orchestrator.apply(setOf(8L, 9L))

        assertEquals(QueryStrategyType.INDIVIDUAL_QUERY, orchestrator.getStrategy())
        assertEquals(setOf(8L, 9L), orchestrator.getIDs())
    }

    @Test
    fun `apply(Set) forces SHARED_QUERY when individual query is disabled`() {
        every { dataLoggerSettings.instance() } returns
            DataLoggerSettings(adapter = Adapter(individualQueryStrategyEnabled = false))

        orchestrator.setStrategy(QueryStrategyType.PERFORMANCE_QUERY)
        orchestrator.apply(setOf(8L, 9L))

        assertEquals(QueryStrategyType.SHARED_QUERY, orchestrator.getStrategy())
    }
}
