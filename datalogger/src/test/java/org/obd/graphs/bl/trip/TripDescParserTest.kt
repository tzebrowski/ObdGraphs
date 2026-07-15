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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.obd.graphs.bl.TestSetup
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class TripDescParserTest : TestSetup() {

    private lateinit var parser: TripDescParser

    @Before
    override fun setup() {
        super.setup()
        parser = TripDescParser()
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `decodeTripName splits prefix, profile, startTime and length`() {
        val parts = parser.decodeTripName("trip-profile_5-1700000000-120.jsonl")

        assertEquals(listOf("trip", "profile_5", "1700000000", "120"), parts)
    }

    @Test
    fun `decodeTripName strips the synced suffix before splitting`() {
        val parts = parser.decodeTripName("trip-profile_5-1700000000-120.jsonl.synced")

        assertEquals(listOf("trip", "profile_5", "1700000000", "120"), parts)
    }

    @Test
    fun `decodeTripName does not throw when there are fewer dash segments`() {
        val parts = parser.decodeTripName("trip-profile_5")

        assertEquals(listOf("trip", "profile_5"), parts)
    }

    @Test
    fun `getTripDesc resolves a known profile label from preferences`() {
        every { sharedPrefs.getString(eq("pref.profile.names.profile_5"), any()) } returns "Track Car"

        val desc = parser.getTripDesc("trip-profile_5-1700000000-120.jsonl")

        assertEquals("trip-profile_5-1700000000-120.jsonl", desc.fileName)
        assertEquals("profile_5", desc.profileId)
        assertEquals("Track Car", desc.profileLabel)
        assertEquals("1700000000", desc.startTime)
        assertEquals("120", desc.tripTimeSec)
        assertFalse(desc.isSynced)
    }

    @Test
    fun `getTripDesc falls back to the numbered default label when preference unset`() {
        // TestSetup's relaxed sharedPrefs returns the default value passed to getString,
        // matching DefaultProfileService's own "Profile N" fallback.
        val desc = parser.getTripDesc("trip-profile_9-1700000000-120.jsonl")

        assertEquals("Profile 9", desc.profileLabel)
    }

    @Test
    fun `getTripDesc marks synced files and defaults missing startTime and tripTimeSec`() {
        val synced = parser.getTripDesc("trip-profile_5.jsonl.synced")

        assertTrue(synced.isSynced)
        assertEquals("", synced.startTime)
        assertEquals("0", synced.tripTimeSec)
    }

    @Test
    fun `getTripDesc resolves Unknown for a profile id outside the known range`() {
        val desc = parser.getTripDesc("trip-not_a_profile-1700000000-120.jsonl")

        assertEquals("Unknown", desc.profileLabel)
    }

    @Test
    fun `getTripDesc throws when the file name has no profile segment`() {
        // Current behavior: p[1] is accessed unconditionally, so a name without a
        // dash-separated profile segment throws rather than degrading gracefully.
        assertThrows(IndexOutOfBoundsException::class.java) {
            parser.getTripDesc("trip.jsonl")
        }
    }
}
