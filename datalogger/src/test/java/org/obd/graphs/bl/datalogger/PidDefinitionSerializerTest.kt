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
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.obd.metrics.pid.PidDefinition
import org.obd.metrics.pid.ValueType
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val PREF_KEY_PREFIX = "pref.pid.registry.overrides.pid"

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PidDefinitionSerializerTest : TestSetup() {

    @Before
    override fun setup() {
        super.setup()
    }

    @After
    fun tearDown() = unmockkAll()

    // Uses the (id, length, formula, mode, pid, units, description, min, max, type) constructor, which
    // sets `formula` to a real (non-null) string, matching how PIDs are actually built from resource
    // files in production. PidDefinition.formula is @NonNull, so a null formula (as left by some of the
    // other constructors) breaks Jackson deserialization.
    private fun pid(id: Long) = PidDefinition(id, 1, "", "01", "010C", "rpm", "Engine RPM", 0, 8000, ValueType.DOUBLE)

    @Test
    fun `serialize writes the pid as json under a per-id preference key`() {
        val slot = slot<String>()
        every { editor.putString(eq("$PREF_KEY_PREFIX.42"), capture(slot)) } returns editor

        pid(42L).serialize()

        assertTrue(slot.captured.contains("Engine RPM"))
        verify { editor.commit() }
    }

    @Test
    fun `deserialize reconstructs the pid from the stored json`() {
        val slot = slot<String>()
        every { editor.putString(eq("$PREF_KEY_PREFIX.42"), capture(slot)) } returns editor
        pid(42L).serialize()

        every { sharedPrefs.getString(eq("$PREF_KEY_PREFIX.42"), any()) } returns slot.captured

        val result = pid(42L).deserialize()

        assertEquals(42L, result?.id)
        assertEquals("Engine RPM", result?.description)
    }

    @Test
    fun `deserialize returns null when nothing is stored`() {
        assertNull(pid(99L).deserialize())
    }

    @Test
    fun `deserialize returns null instead of throwing on malformed json`() {
        every { sharedPrefs.getString(eq("$PREF_KEY_PREFIX.7"), any()) } returns "{not valid json"

        assertNull(pid(7L).deserialize())
    }
}
