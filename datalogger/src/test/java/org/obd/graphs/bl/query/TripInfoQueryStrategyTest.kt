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
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.Pid
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val TRIP_INFO_QUERY_PREF_KEY = "pref.aa.trip_info.pids.selected"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripInfoQueryStrategyTest : TestSetup() {

    private lateinit var strategy: TripInfoQueryStrategy

    @Before
    override fun setup() {
        super.setup()
        strategy = TripInfoQueryStrategy()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getDefaultPIDs returns the fixed hard-coded set of trip info pids`() {
        val defaults = strategy.getDefaultPIDs()

        assertEquals(22, defaults.size)
        assertTrue(defaults.contains(Pid.ENGINE_SPEED_PID_ID.id))
        assertTrue(defaults.contains(Pid.VEHICLE_SPEED_PID_ID.id))
        assertTrue(defaults.contains(Pid.GEAR_ENGAGED_PID_ID.id))
    }

    @Test
    fun `getPIDs reads the selection from preferences`() {
        every { sharedPrefs.getStringSet(TRIP_INFO_QUERY_PREF_KEY, any()) } returns setOf("13", "14")

        assertEquals(setOf(13L, 14L), strategy.getPIDs())
    }
}
