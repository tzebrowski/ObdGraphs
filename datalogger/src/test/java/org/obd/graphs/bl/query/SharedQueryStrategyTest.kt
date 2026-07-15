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

private const val PREFERENCE_PID_FAST = "pref.pids.generic.high"
private const val PREFERENCE_PID_SLOW = "pref.pids.generic.low"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SharedQueryStrategyTest : TestSetup() {

    private lateinit var strategy: SharedQueryStrategy

    @Before
    override fun setup() {
        super.setup()
        strategy = SharedQueryStrategy()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getPIDs unions fast and slow pid preferences`() {
        every { sharedPrefs.getStringSet(PREFERENCE_PID_FAST, any()) } returns setOf("1", "2")
        every { sharedPrefs.getStringSet(PREFERENCE_PID_SLOW, any()) } returns setOf("3")

        assertEquals(setOf(1L, 2L, 3L), strategy.getPIDs())
    }

    @Test
    fun `getPIDs silently drops non-numeric entries from the slow preference`() {
        every { sharedPrefs.getStringSet(PREFERENCE_PID_FAST, any()) } returns setOf("1")
        every { sharedPrefs.getStringSet(PREFERENCE_PID_SLOW, any()) } returns setOf("2", "not-a-number")

        assertEquals(setOf(1L, 2L), strategy.getPIDs())
    }

    @Test(expected = NumberFormatException::class)
    fun `getPIDs throws for non-numeric entries in the fast preference (current, unguarded behavior)`() {
        every { sharedPrefs.getStringSet(PREFERENCE_PID_FAST, any()) } returns setOf("not-a-number")
        every { sharedPrefs.getStringSet(PREFERENCE_PID_SLOW, any()) } returns emptySet()

        strategy.getPIDs()
    }
}
