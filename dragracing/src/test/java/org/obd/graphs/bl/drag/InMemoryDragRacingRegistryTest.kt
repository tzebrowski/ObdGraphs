 /**
 * Copyright 2019-2025, Tomasz Żebrowski
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
package org.obd.graphs.bl.drag

import android.content.ContextWrapper
import android.os.Environment
import android.util.Log
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updateString
import java.lang.ref.WeakReference

class InMemoryDragRacingRegistryTest {

    private lateinit var registry: InMemoryDragRacingRegistry
    private val mockContext = mockk<ContextWrapper>(relaxed = true)

    @Before
    fun setup() {
        // 1. Mock System (Log/Environment)
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns true
        every { Log.v(any(), any()) } returns 0

        mockkStatic(Environment::class)
        every { Environment.getExternalStorageState() } returns Environment.MEDIA_MOUNTED

        mockkStatic("org.obd.graphs.preferences.PreferencesKt")
        mockkStatic("org.obd.graphs.ContextKt")

        val field = Class.forName("org.obd.graphs.ContextKt")
            .getDeclaredField("activityContext")
        field.isAccessible = true
        field.set(null, WeakReference(mockContext))

        mockkObject(Prefs)
        every { Prefs.getString(any(), any()) } returns null
        registry = InMemoryDragRacingRegistry()
    }

    @After
    fun teardown() {
        unmockkAll()
    }

//    @Test
    fun `update100200 correctly saves data`() {
        val metric = DragRacingMetric(time = 5500L, speed = 200)
        registry.update100200(metric)

        verify { Prefs.updateString(match { it.contains("100_200") && it.contains("best") }, "5500") }
        assert(registry.getResult()._100_200.best == 5500L)
    }

//    @Test
    fun `update60140 correctly saves data`() {
        val metric = DragRacingMetric(time = 4200L, speed = 140)
        registry.update60140(metric)
        verify { Prefs.updateString(match { it.contains("60_140") && it.contains("best") }, "4200") }
        assert(registry.getResult()._60_140.best == 4200L)
    }

    @Test
    fun `Shift light threshold is gettable and settable`() {
        // Default check
        assert(registry.getShiftLightsRevThreshold() == 5000)

        // Update
        registry.setShiftLightsRevThreshold(7000)

        // Verify
        assert(registry.getShiftLightsRevThreshold() == 7000)
    }

    @Test
    fun `State flags (ReadyToRace, ShiftLights) update result object`() {
        // Initial state
        assert(!registry.getResult().readyToRace)
        assert(!registry.getResult().enableShiftLights)

        // Change State
        registry.readyToRace(true)
        registry.enableShiftLights(true)

        // Verify Result Object reflects change
        assert(registry.getResult().readyToRace)
        assert(registry.getResult().enableShiftLights)
    }
}
