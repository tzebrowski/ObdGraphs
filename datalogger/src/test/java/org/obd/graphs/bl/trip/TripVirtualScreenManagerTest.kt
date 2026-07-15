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
package org.obd.graphs.bl.trip

import io.mockk.every
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val VIRTUAL_SCREEN_SELECTION = "pref.graph.virtual.selected"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripVirtualScreenManagerTest : TestSetup() {

    private lateinit var manager: TripVirtualScreenManager

    @Before
    override fun setup() {
        super.setup()
        manager = TripVirtualScreenManager()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `getCurrentScreenId defaults to 1 when no preference is stored`() {
        assertEquals("1", manager.getCurrentScreenId())
    }

    @Test
    fun `getCurrentScreenId reflects the stored preference`() {
        every { sharedPrefs.getString(eq(VIRTUAL_SCREEN_SELECTION), any()) } returns "3"

        assertEquals("3", manager.getCurrentScreenId())
    }

    @Test
    fun `updateScreenId writes the given screen id to preferences`() {
        manager.updateScreenId("2")

        verify { editor.putString(eq(VIRTUAL_SCREEN_SELECTION), eq("2")) }
    }

    @Test
    fun `updateScreenId defaults to persisting the current screen id when none is given`() {
        every { sharedPrefs.getString(eq(VIRTUAL_SCREEN_SELECTION), any()) } returns "4"

        manager.updateScreenId()

        verify { editor.putString(eq(VIRTUAL_SCREEN_SELECTION), eq("4")) }
    }

    @Test
    fun `getVirtualScreenPrefKey is scoped to the current screen id`() {
        every { sharedPrefs.getString(eq(VIRTUAL_SCREEN_SELECTION), any()) } returns "5"

        assertEquals("pref.graph.pids.selected.5", manager.getVirtualScreenPrefKey())
    }

    @Test
    fun `getCurrentMetrics reads from the current screen's preference key`() {
        every { sharedPrefs.getString(eq(VIRTUAL_SCREEN_SELECTION), any()) } returns "5"
        every { sharedPrefs.getStringSet(eq("pref.graph.pids.selected.5"), any()) } returns setOf("13", "14")

        assertEquals(setOf("13", "14"), manager.getCurrentMetrics())
    }

    @Test
    fun `updateReservedVirtualScreen always writes to the reserved screen key`() {
        every { sharedPrefs.getString(eq(VIRTUAL_SCREEN_SELECTION), any()) } returns "5"

        manager.updateReservedVirtualScreen(listOf("13", "14"))

        verify { editor.putStringSet(eq("pref.graph.pids.selected.6"), eq(setOf("13", "14"))) }
    }
}
