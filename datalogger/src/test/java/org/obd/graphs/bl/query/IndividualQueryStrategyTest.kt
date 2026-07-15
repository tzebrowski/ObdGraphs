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
import org.obd.graphs.PREF_DYNAMIC_SELECTOR_ENABLED
import org.obd.graphs.bl.TestSetup
import org.obd.graphs.bl.datalogger.Pid
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class IndividualQueryStrategyTest : TestSetup() {

    private lateinit var strategy: IndividualQueryStrategy

    @Before
    override fun setup() {
        super.setup()
        strategy = IndividualQueryStrategy()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getPIDs does not add the dynamic selector pid when the preference is disabled`() {
        every { sharedPrefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false) } returns false
        strategy.update(setOf(1L, 2L))

        assertEquals(setOf(1L, 2L), strategy.getPIDs())
    }

    @Test
    fun `getPIDs adds the dynamic selector pid when the preference is enabled, keeping prior pids`() {
        every { sharedPrefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false) } returns true
        strategy.update(setOf(1L, 2L))

        val result = strategy.getPIDs()

        assertTrue(result.containsAll(setOf(1L, 2L, Pid.DYNAMIC_SELECTOR_PID_ID.id)))
        assertEquals(3, result.size)
    }

    @Test
    fun `getPIDs does not duplicate the dynamic selector pid across repeated calls`() {
        every { sharedPrefs.getBoolean(PREF_DYNAMIC_SELECTOR_ENABLED, false) } returns true

        strategy.getPIDs()
        val result = strategy.getPIDs()

        assertEquals(1, result.count { it == Pid.DYNAMIC_SELECTOR_PID_ID.id })
    }
}
