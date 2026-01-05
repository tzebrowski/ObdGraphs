 /**
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
package org.obd.graphs.profile

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.obd.graphs.preferences.Prefs
import org.obd.graphs.preferences.updatePreference
import java.io.ByteArrayInputStream

internal class ProfileServiceTest : TestSetup() {
    @Before
    override fun setup() {
        super.setup()

        mockkStatic("org.obd.graphs.preferences.PreferencesKt")
        every { Prefs } returns sharedPrefs
        every { sharedPrefs.edit() } returns editor
        every { sharedPrefs.all } returns emptyMap()
        every { sharedPrefs.getString(any(), any()) } answers { secondArg() }
        every { sharedPrefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { sharedPrefs.registerOnSharedPreferenceChangeListener(any()) } just Runs
    }

    @Test
    fun `getAvailableProfiles returns map of default profiles`() {
        // Arrange
        // When the code asks for a profile name, return a specific value
        every { sharedPrefs.getString("pref.profile.names.profile_1", any()) } returns "User Profile 1"

        // Act
        val profiles = profileService.getAvailableProfiles()

        // Assert
        assertEquals(20, profiles.size)
        assertEquals("User Profile 1", profiles["profile_1"])
        assertEquals("Profile 2", profiles["profile_2"])
    }

    @Test
    fun `getCurrentProfile returns default if not set`() {
        // Arrange
        every { sharedPrefs.getString("pref.profile.id", "profile_1") } returns "profile_1"

        // Act
        val current = profileService.getCurrentProfile()

        // Assert
        assertEquals("profile_1", current)
    }

    @Test
    fun `updateCurrentProfileName saves name to prefs`() {
        // Arrange
        val newName = "My Race Mode"
        every { sharedPrefs.getString("pref.profile.id", any()) } returns "profile_1"

        // Act
        profileService.updateCurrentProfileName(newName)

        // Assert
        verify {
            editor.putString("pref.profile.names.profile_1", newName)
        }
    }

    @Test
    fun `saveCurrentProfile filters reserved keys and saves rest with prefix`() {
        // Arrange
        val currentProfile = "profile_1"
        every { sharedPrefs.getString("pref.profile.id", any()) } returns currentProfile

        // Simulate existing preferences
        val mockMap =
            mapOf(
                "user_setting_1" to "value1",
                "profile_2.setting" to "ignore_me",
                "pref.profile.names.p1" to "ignore_me",
                "pref.about.build" to "value2",
            )
        every { sharedPrefs.all } returns mockMap

        // Act
        profileService.saveCurrentProfile()

        // Assert
        verify {
            editor.updatePreference("profile_1.user_setting_1", "value1")
            editor.updatePreference("profile_1.pref.about.build", "value2")
        }

        // Verify reserved keys were NOT saved
        verify(exactly = 0) {
            editor.updatePreference(match { it.startsWith("profile_1.profile_") }, any())
        }
    }

    @Test
    fun `loadProfile loads values from prefix and resets others`() {
        // Arrange
        val targetProfile = "profile_2"

        val mockMap =
            mapOf(
                "profile_2.engine_type" to "V8",
                "profile_2.color" to "Red",
                "profile_1.engine_type" to "V6",
                "generic_setting" to "default",
            )
        every { sharedPrefs.all } returns mockMap

        // Act
        profileService.loadProfile(targetProfile)

        // Assert
        verify { editor.remove("generic_setting") }
        verify { editor.updatePreference("engine_type", "V8") }
        verify { editor.updatePreference("color", "Red") }

        // Verify we ignored profile_1
        verify(exactly = 0) { editor.updatePreference("engine_type", "V6") }

        // Verify current profile name update
        verify { editor.putString("pref.profile.current_name", any()) }
    }

    @Test
    fun `test saveCurrentProfile saves root changes back to profile_3`() {
        mockkObject(Prefs)
        every { Prefs.edit() } returns editor
        every { Prefs.all } returns emptyMap()
        every { Prefs.getString(any(), any()) } answers { secondArg() as String }
        every { Prefs.getBoolean(any(), any()) } returns false // Default to false for installation check
        every { Prefs.registerOnSharedPreferenceChangeListener(any()) } just Runs

        // Mock Editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putStringSet(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.commit() } returns true
        every { editor.apply() } just Runs

        // Mock AssetManager to return our file names and content
        every { assets.list("") } returns arrayOf("alfa_2_0_gme.properties", "alfa_175_tbi.properties")

        every { assets.open("alfa_2_0_gme.properties") } answers {
            ByteArrayInputStream(ALFA_2_0_GME_CONTENT.toByteArray())
        }
        every { assets.open("alfa_175_tbi.properties") } answers {
            ByteArrayInputStream(ALFA_175_TBI_CONTENT.toByteArray())
        }

        // Arrange: We are currently in profile_3
        every { Prefs.getString("pref.profile.id", any()) } returns "profile_3"

        // Simulating the root preferences (what the app is currently using)
        val currentRootPrefs =
            mapOf(
                "pref.adapter.init.protocol" to "CAN_11_MODIFIED", // User changed this
                "pref.gauge.fps" to "10",
                "profile_3.pref.original" to "original", // Should be ignored
            )
        every { Prefs.all } returns currentRootPrefs

        // Act
        profileService.saveCurrentProfile()

        // Assert
        // Verify the user's change was written back to "profile_3.pref.adapter.init.protocol"
        verify { editor.updatePreference("profile_3.pref.adapter.init.protocol", "CAN_11_MODIFIED") }
        verify { editor.updatePreference("profile_3.pref.gauge.fps", "10") }
    }
}
