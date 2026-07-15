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

import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class QueryStrategyTest : TestSetup() {

    private lateinit var strategy: QueryStrategy

    @Before
    override fun setup() {
        super.setup()
        strategy = QueryStrategy()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getDefaultPIDs returns empty set by default`() {
        assertTrue(strategy.getDefaultPIDs().isEmpty())
    }

    @Test
    fun `getPIDs returns empty set initially`() {
        assertTrue(strategy.getPIDs().isEmpty())
    }

    @Test
    fun `update replaces the pids set`() {
        strategy.update(setOf(1L, 2L, 3L))
        assertEquals(setOf(1L, 2L, 3L), strategy.getPIDs())

        strategy.update(setOf(4L))
        assertEquals(setOf(4L), strategy.getPIDs())
    }

    @Test
    fun `update on a constructor-seeded strategy replaces the seeded pids entirely`() {
        val seeded = QueryStrategy(mutableSetOf(9L))
        seeded.update(setOf(1L))
        assertEquals(setOf(1L), seeded.getPIDs())
    }
}
