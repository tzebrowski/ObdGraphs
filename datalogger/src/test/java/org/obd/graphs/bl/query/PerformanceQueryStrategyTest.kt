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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PERFORMANCE_QUERY_PREF_KEY = "pref.aa.performance.pids.selected"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PerformanceQueryStrategyTest : TestSetup() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getDefaultPIDs combines top, bottom and brake boosting pids`() {
        every { sharedPrefs.getStringSet(PREF_QUERY_PERFORMANCE_TOP, any()) } returns setOf("10", "11")
        every { sharedPrefs.getStringSet(PREF_QUERY_PERFORMANCE_BOTTOM, any()) } returns setOf("20")
        every { sharedPrefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_GAS_METRIC, -1) } returns 30
        every { sharedPrefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_ARBITRARY_METRIC, -1) } returns 31
        every { sharedPrefs.getInt(PREF_QUERY_PERFORMANCE_BRAKE_BOOSTING_VEHICLE_SPEED_METRIC, -1) } returns 32

        val strategy = PerformanceQueryStrategy()

        assertEquals(setOf(10L, 11L, 20L, 30L, 31L, 32L), strategy.getDefaultPIDs())
    }

    @Test
    fun `getDefaultPIDs falls back to the -1 marker when brake boosting prefs are unset`() {
        every { sharedPrefs.getStringSet(any(), any()) } returns emptySet()
        every { sharedPrefs.getInt(any(), any()) } returns -1

        val strategy = PerformanceQueryStrategy()

        assertEquals(setOf(-1L), strategy.getDefaultPIDs())
    }

    @Test
    fun `getPIDs reads from the performance selection preference`() {
        every { sharedPrefs.getStringSet(PERFORMANCE_QUERY_PREF_KEY, any()) } returns setOf("100", "200")

        val strategy = PerformanceQueryStrategy()

        assertEquals(setOf(100L, 200L), strategy.getPIDs())
    }
}
